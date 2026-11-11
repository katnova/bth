class Bth < Formula
  desc "Better terminal history manager"
  homepage "https://git.akat.xyz/bth"
  url "https://git.akat.xyz/bth.git", branch: "master"
  version "0.0.195"
  license "BSD-3-Clause"

  depends_on "openjdk@25" => :build
  depends_on arch: :arm64
  depends_on "confuse"
  depends_on :macos
  depends_on "notcurses"

  uses_from_macos "sqlite"

  def install
    ENV["JAVA_HOME"] = Language::Java.java_home("25")
    ENV["GRADLE_USER_HOME"] = buildpath/".gradle"
    ENV["KONAN_DATA_DIR"] = buildpath/".konan"

    system "./gradlew", "--no-daemon", "linkReleaseExecutableMacosArm64",
           "-PbrewPrefix=#{HOMEBREW_PREFIX}"
    bin.install "build/bin/macosArm64/releaseExecutable/bth.kexe" => "bth"
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/bth version")
  end
end