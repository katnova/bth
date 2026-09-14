# bth - Better Terminal History

**bth** is a real-time terminal history manager for ZSH, built with Kotlin/Native.  
It captures shell commands as they run, stores them in a local SQLite database, and
presents them through an interactive TUI powered by
[notcurses](https://github.com/dankamongmen/notcurses).

> **Alpha status:** bth is not feature-complete and should not be considered stable
> for everyday use. This project started in May 2026. Everything here is subject to
> change; large parts of the codebase have already been rewritten several times.
> 
> Until beta, breaking changes may be frequent.

https://web.mit.edu/gnu/doc/html/standards_18.html
https://www.gnu.org/prep/standards/standards.html

---

## VCS

The "source" repo is at git.akat.xyz/bth (CI is here as well). For issues, please open them either on codeberg or github. I sign all of my commits with my public gpg key (`3902BCEB8BFD5E8F`).

git.akat.xyz: https://git.akat.xyz/bth

Codeberg: https://codeberg.org/akat/bth

GitHub: https://github.com/katnova/bth

---

## Why?

For the love of it.  

I've been using [Atuin](https://github.com/atuinsh/atuin) for years, but it falls
short in a few areas of personal preference, so I decided to build my own.

The goal is an **easily extensible** (at a code level), **customizable**, **performant**, and
**well-documented** tool with sane defaults written entirely by hand.

---

## Features

- **No AI/LLM-generated code**
- Real-time ZSH command capture via shell hooks (`preexec` / `precmd`)
- SQLite-backed persistent history storage
- Scrollable, interactive TUI for browsing history (via notcurses)
- Tracks elevated privileges, working directory, exit status, and more
- Configurable via an INI-style config file (via libconfuse)
- Minimal dependencies

---

## Support

bth is tested on the following terminals during development:

- Kitty
- Alacritty
- Ghostty
- mlterm
- Konsole
- Terminator
- Yakuake

---

## Dependencies

bth links against three C libraries:

| Library                                                | Purpose                    |
|--------------------------------------------------------|----------------------------|
| [notcurses](https://github.com/dankamongmen/notcurses) | TUI rendering              |
| [sqlite3](https://www.sqlite.org/)                     | History database           |
| [libconfuse](https://github.com/martinh/libconfuse)    | Configuration file parsing |

The only Kotlin dependency is `kotlinx-coroutines-core-linuxx64`; everything else
is pure Kotlin/Native.

**Arch Linux:**

At TOW, I'm using ldd (GNU libc) 2.43.

```sh
sudo pacman -S notcurses sqlite libconfuse
```

> Other distributions are not yet officially supported, multi-distro support is on
> the long-term roadmap. They may or may not work. Feel free to let me know via an issue.

---

## Building

bth uses [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
with Gradle, targeting Linux x86-64.

```sh
./gradlew linuxX64Binaries
```

The release binary is placed at:

```
build/bin/linuxX64/releaseExecutable/bth.kexe
```

---

## Installation

Use the provided install script to copy the binary to `/usr/local/bin`:

```sh
bash shell/install.sh
```

The script derives the project root from its own location, so it works from any
c< ALL >heckout path.

---

## ZSH Integration

Add the following to your `.zshrc`:

```zsh
eval "$(bth init --shell zsh)"
```

This installs `preexec` and `precmd` hooks that record each command along with its
context - working directory, exit status, and sudo flag - into the SQLite database.

---

## Roadmap

The high-level features/items and at which stage they're anticipated to be completed at.

### Alpha

- [X] PKGBUILD (not pushed to AUR)
- [X] Cleanup and squash current git history 
- [X] `fish` support
- [X] `zsh` support 
- [X] CatchyOS support
- [X] ArchLinux support
- [X] Session tracking (shell-agnostic)
- [X] Qodana
- [ ] `bash` support
- [ ] Bookmarks
- [ ] Sequences


### Beta

- [ ] bth.sh, docs/basic how-to's/asciinema recording demo
- [ ] Unit, integration, and end-to-end tests
- [ ] Shell tracking
- [ ] Git repo/branch tracking
- [ ] Bash history import
- [ ] ZSH / Atuin history import
- [ ] Fuzzy search (fzf-style)
- [ ] Ubuntu support
- [ ] NixOS support
- [ ] Fedora support

### Long-term

- [ ] Configurable storage backend with a decoupled storage layer
- [ ] Multi-distro support
- [ ] Cross-platform support (macOS first, Windows later)
- [ ] Multi-device sync (w/o a central 'server')

## License

BSD 3-Clause - Copyright © 2026 akat@akat.xyz  

See [LICENSE](LICENSE) for the full license text.
