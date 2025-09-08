#!/bin/bash

[[ "TRACE" ]] && set -x

: ${REALM:=EXAMPLE}
: ${DOMAIN_REALM:=example}
: ${KERB_MASTER_KEY:=masterkey}
: ${KERB_ADMIN_USER:=admin}
: ${KERB_ADMIN_PASS:=admin}
: ${SEARCH_DOMAINS:=ldap.example.org}
: ${LDAP_DC:=dc=example,dc=org}
: ${LDAP_USER:=admin}
: ${LDAP_PASS:=admin}
: ${LDAP_URL:=ldap://$SEARCH_DOMAINS}

create_user() {
  echo "########### Creating ldap records ##############"
  ldapadd -x -H ldap://ldap.example.org:1389 -D "cn=admin,dc=example,dc=org" -W -f /home/init_ldap.ldif<<EOF
$LDAP_PASS
EOF
  
  echo "########### Creating service principal for kafka ##############"
  kadmin.local -q "add_principal -x linkdn=cn=kafka,OU=ServiceAccount,OU=Kafka,OU=Prod,OU=Infrastructure,DC=example,DC=org -pw mypassword kafka/kafka"

  echo "########### Creating service principal for user123 ##############"
  kadmin.local -q "add_principal -x linkdn=cn=user123,OU=ServiceAccount,OU=Kafka,OU=Prod,OU=Infrastructure,DC=example,DC=org -pw mypassword user123"

  echo "########### Exporting Kerberos Ticket for kafka ##############"
  kadmin.local -q "ktadd -k /tmp/kafka.service.keytab kafka/kafka@EXAMPLE.ORG"
  
  echo "###### Check Kafka Ticket on server side"
  ls -l /tmp/
}

fix_nameserver() {
  cat>/etc/resolv.conf<<EOF
nameserver $NAMESERVER_IP
search $SEARCH_DOMAINS
EOF
}

create_db() {
  kdb5_util -P $KERB_MASTER_KEY -r $REALM create -s
}

init_ldap() {
  kdb5_ldap_util -D cn=$LDAP_USER,$LDAP_DC create -subtrees $LDAP_DC -r $REALM -s -H $LDAP_URL <<EOF
$LDAP_PASS
$KERB_ADMIN_PASS
$KERB_ADMIN_PASS
EOF

  kdb5_ldap_util -D cn=$LDAP_USER.,$LDAP_DC stashsrvpw -f /etc/krb5kdc/service.keyfile cn=$LDAP_USER,$LDAP_DC <<EOF
$LDAP_PASS
$LDAP_PASS
$LDAP_PASS
EOF
}

start_kdc() {
  service krb5-kdc start
  service krb5-admin-server start
}

restart_kdc() {
  service krb5-kdc restart
  service krb5-admin-server restart
}

create_admin_user() {
  echo "##### createing admin user"
  kadmin.local -q "addprinc -x dn=cn=$KERB_ADMIN_USER,$LDAP_DC admin" <<EOF
$LDAP_PASS
$LDAP_PASS
EOF
  echo "*/admin@$REALM *" > /etc/krb5kdc/kadm5.acl
}

if [ ! -f /kerberos_initialized ]; then
  mkdir -p /var/log/kerberos

  init_ldap
  create_admin_user
  create_db
  start_kdc
  create_user

  touch /kerberos_initialized
else
  start_kdc
  create_user
fi

tail -F /var/log/kerberos/krb5kdc.log
