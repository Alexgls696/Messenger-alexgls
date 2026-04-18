FROM postgres:16

RUN apt-get update && apt-get install -y \
    build-essential \
    postgresql-server-dev-16 \
    git \
    && rm -rf /var/lib/apt/lists/*

RUN git clone https://github.com/pgpartman/pg_partman.git /tmp/pg_partman \
    && cd /tmp/pg_partman \
    && make install \
    && rm -rf /tmp/pg_partman

RUN apt-get purge -y --auto-remove build-essential postgresql-server-dev-16 git
