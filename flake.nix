{
  description = "A simple file manager intended to be used in combination with TUI editors like Neovim, Helix, Kakoune.";

  inputs = {
    flake-utils.url = "github:numtide/flake-utils";
    sbt.url = "github:zaninime/sbt-derivation";
    sbt.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = {
    nixpkgs,
    flake-utils,
    sbt,
    ...
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = import nixpkgs {inherit system;};
      kmtm = sbt.lib.mkSbtDerivation {
        inherit pkgs;
        pname = "kmtm";
        version = "1.0.0";
        src = ./.;
        depsSha256 = "sha256-wNkZzyIlnBVPJ+JbTe1ASQ/ZOZnOVO+NHNt6IDNPD+k=";
        nativeBuildInputs = [pkgs.graalvm-ce];
        buildPhase = "sbt 'GraalVMNativeImage/packageBin'";
        installPhase = ''
          mkdir -p $out/bin
          mv target/graalvm-native-image/kmtm $out/bin/
        '';
      };
    in {
      formatter = pkgs.alejandra;
      packages.default = kmtm;
      apps.default = {
        type = "app";
        program = "${kmtm}/bin/kmtm";
      };
    });
}
