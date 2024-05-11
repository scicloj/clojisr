FROM mcr.microsoft.com/devcontainers/java:21-bookworm

RUN bash -c "bash < <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install)"
RUN bash -c "bash < <(curl -s https://raw.githubusercontent.com/clojure-lsp/clojure-lsp/master/install)"
RUN apt-get update && apt-get install -y rlwrap gfortran libblas-dev liblapack-dev  libpng-dev libfontconfig1-dev libfreetype-dev gfortran  libicu-dev cmake libxi6
