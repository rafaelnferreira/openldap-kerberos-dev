FROM ubuntu

# kerberos
RUN apt-get update -y && apt-get install -y krb5-kdc-ldap krb5-admin-server ldap-utils vim openssh-client

EXPOSE 88 464 749

RUN mkdir -p /etc/openldap/certs

# ADD ./ldap/ldap.conf /etc/ldap/ldap.conf
ADD ./entrypoint.sh /entrypoint.sh
# ADD ./init_ldap.ldif /home

RUN chmod 644 /etc/ldap/ldap.conf && chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
