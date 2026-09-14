hyperfine --warmup 500 \
  'bth precmd --dir "/home/akat" --elv 0 --exc 0 --bhi "$(bth preexec --dir /home/akat/IdeaProjects/bth/shell --elv 0 --ert 1785383158.2486610413 --ses 102 --cmd "Hyperfine test woooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo :D")" --ert 1785383258.2486610413' \
  'ATUIN_LOG=error atuin history end --exit 134 --duration=1000000000 -- "$(atuin history start -- Hyperfine test woooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo :D)"'
