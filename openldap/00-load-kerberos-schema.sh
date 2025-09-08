#!/bin/bash
set -euo pipefail

echo "🔧 [OpenLDAP] Offline-loading Kerberos schema and initial entries with slapadd..."

CNCONFIG_DIR="/opt/bitnami/openldap/etc/slapd.d"

# Load schema into cn=config (DB 0)
slapadd -F "$CNCONFIG_DIR" -n 0 -l /docker-entrypoint-initdb.d/kerberos.ldif

echo "🎉 [OpenLDAP] Schema init completed. Base DIT LDIFs will be loaded from /ldifs by Bitnami."
