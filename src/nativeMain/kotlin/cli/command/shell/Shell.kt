package cli.command

import cli.OptSpec
import cli.ParsedArgs

data object Shell : Command {
    override val names = setOf("init")
    override val cmdHelp = "Prints the shell script for install BTH hooks. Intended to be `eval`'d."

    val shell = OptSpec.Value(name = "shell", convert = { it }, help = "The name of the shell, i.e. zsh")
    override val spec = listOf(shell)

    override fun execute(args: ParsedArgs) {
        when (args.value(shell)) {
            "zsh" -> {
                println(
                    $$"""
# Needs to be sourced in ~/.zshrc

# I'm not sure if this is really applicable 'in the real world', but in my VM, somethings screwy with ZSH, and $EPOCHREALTIME doesn't populate. This resolves that.
zmodload zsh/datetime

autoload -U add-zsh-hook

BTH_HISTORY_ID=""
BTH_SESSION_ID=$(bth session --ert "$EPOCHREALTIME")

_bth_preexec() {
  local cid
  local elevated=0
  
  # Check if running with elevated privileges
  if sudo -n true 2>/dev/null; then
    elevated=1
  fi
  
  # POSITIONAL!!! Changing order WILL break parsing.
  cid=$(bth preexec --dir "$(pwd)" --elv "$elevated" --ert "$EPOCHREALTIME" --ses "$BTH_SESSION_ID" --cmd "$1" )
  export BTH_HISTORY_ID="$cid"
}

_bth_precmd() {
  local EXIT="$?"
  local elevated=0

  [[ -z "${BTH_HISTORY_ID:-}" ]] && return
  
  [[ "$BTH_HISTORY_ID" == "-1" ]] && echo "BTH encountered an error saving command exit code and end time" && return

  # Check if running with elevated privileges
  if sudo -n true 2>/dev/null; then
    elevated=1
  fi

  bth precmd --dir "$(pwd)" --elv "$elevated" --exc $EXIT --bhi "$BTH_HISTORY_ID" --ert "$EPOCHREALTIME"
}

_bth_history() {
  zle -I

  local ttmp
  local etmp

  ttmp="$(mktemp)"
  etmp="$(mktemp)"

  bth tui --ttmp "$ttmp" --etmp "$etmp" --pwd "$(pwd)" --cmd "$BUFFER" </dev/tty >/dev/tty 2>/dev/tty

  if [[ -s "$ttmp" ]]; then
    BUFFER="$(cat "$ttmp")"
  fi

  if [[ -s "$etmp" ]]; then
    BUFFER="$(cat "$etmp")"
    zle accept-line
  fi
  
  rm -f "$ttmp"
  rm -f "$etmp"
  
  CURSOR=${#BUFFER}

  zle reset-prompt
}

zle -N _bth_history

bindkey '^[[A' _bth_history
bindkey '^[OA' _bth_history

add-zsh-hook preexec _bth_preexec
add-zsh-hook precmd _bth_precmd                    
                """.trimIndent()
                )
            }
            "fish" -> {
                println(
                    $$"""
# Needs to be sourced in ~/.config/fish/config.fish

set -g BTH_HISTORY_ID ""
set -g BTH_SESSION_ID (bth session --ert (date +%s.%N))

function _bth_preexec --on-event fish_preexec
  set -l cid
  set -l elevated 0

  # Check if running with elevated privileges
  if sudo -n true 2>/dev/null
    set elevated 1
  end

  # POSITIONAL!!! Changing order WILL break parsing.
  set cid (bth preexec --dir (pwd) --elv "$elevated" --ert (date +%s.%N) --ses "$BTH_SESSION_ID" --cmd "$argv[1]")
  set -gx BTH_HISTORY_ID "$cid"
end

function _bth_precmd --on-event fish_postexec
  set -l EXIT $status
  set -l elevated 0

  test -z "$BTH_HISTORY_ID"; and return

  test "$BTH_HISTORY_ID" = "-1"; and echo "BTH encountered an error saving command exit code and end time"; and return

  # Check if running with elevated privileges
  if sudo -n true 2>/dev/null
    set elevated 1
  end

  bth precmd --dir (pwd) --elv "$elevated" --exc "$EXIT" --bhi "$BTH_HISTORY_ID" --ert (date +%s.%N)
end

function _bth_history
  set -l ttmp
  set -l etmp

  set ttmp (mktemp)
  set etmp (mktemp)

  bth tui --ttmp "$ttmp" --etmp "$etmp" --pwd (pwd) --cmd (commandline) </dev/tty >/dev/tty 2>/dev/tty

  if test -s "$ttmp"
    commandline (cat "$ttmp")
  end

  if test -s "$etmp"
    commandline (cat "$etmp")
    commandline -f execute
  end

  rm -f "$ttmp"
  rm -f "$etmp"

  commandline -f repaint
end

bind \e\[A _bth_history
bind \eOA _bth_history
                """.trimIndent()
                )
            }
        }
    }
}
