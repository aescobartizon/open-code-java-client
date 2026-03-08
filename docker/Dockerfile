# syntax=docker/dockerfile:1.6
FROM node:20-bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    git \
    openssh-client \
    curl \
    tini \
  && rm -rf /var/lib/apt/lists/*

RUN npm install -g opencode-ai

# Falla el build si el binario nativo no existe (evita ENOENT en runtime)
RUN set -eux; \
    GLOBAL_ROOT="$(npm root -g)"; \
    ls -la "${GLOBAL_ROOT}/opencode-ai/bin"; \
    test -f "${GLOBAL_ROOT}/opencode-ai/bin/.opencode"

# Crear usuario no-root (sin UID fijo) o reutilizar si ya existe
RUN set -eux; \
    if ! id -u opencode >/dev/null 2>&1; then \
      useradd -m -s /bin/bash opencode; \
    fi; \
    mkdir -p /home/opencode/.config/opencode /workspaces; \
    chown -R opencode:opencode /home/opencode /workspaces

USER opencode
ENV HOME=/home/opencode
ENV XDG_CONFIG_HOME=/home/opencode/.config
WORKDIR /workspaces

EXPOSE 4096
ENTRYPOINT ["tini","--"]
CMD ["opencode","serve","--hostname","0.0.0.0","--port","4096"]


