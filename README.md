# Kerberos and LDAP sample for local development

This project sets up a local development environment with OpenLDAP and Kerberos KDC integrated together.

## Quick Start

```bash
docker compose up -d --build
```

The OpenLDAP container loads the Kerberos schema (via offline slapadd into cn=config) and creates initial users automatically during its init phase (Bitnami init directory).

Then bring up Kerberos when ready (after LDAP is initialized).

## Available Users

The setup creates several users with Kerberos principals:

- **user123** (Principal: user123@EXAMPLE.ORG)
- **kafka** (Principal: kafka/kafka@EXAMPLE.ORG)  

## Services

- **OpenLDAP**: Available on port 1389 (admin/admin)
- **Kerberos KDC**: Available on port 1188/udp
- **Kerberos Admin**: Available on port 11749

## Connecting and obtaining a ticket

```bash
export KRB5_CONFIG=$PWD/krb5-client.conf

echo mypassword | kinit --password-file=STDIN user123@EXAMPLE.ORG

```
