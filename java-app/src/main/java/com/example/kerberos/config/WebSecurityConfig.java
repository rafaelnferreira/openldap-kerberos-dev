package com.example.kerberos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.client.config.SunJaasKrb5LoginConfig;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Value("${app.ad-domain:ORG.EXAMPLE}")
	private String adDomain;

	@Value("${app.ad-server:ldap://ldap.example.org:1389}")
	private String adServer;

	@Value("${app.service-principal:HTTP/example.org@EXAMPLE.ORG}")
	private String servicePrincipal;

	@Value("${app.keytab-location:./java-app.keytab}")
	private String keytabLocation;

	@Value("${app.ldap-search-base:dc=example,dc=org}")
	private String ldapSearchBase;

	@Value("${app.ldap-search-filter:uid={0}}")
	private String ldapSearchFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider = kerberosServiceAuthenticationProvider();
		ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider = activeDirectoryLdapAuthenticationProvider();
		ProviderManager providerManager = new ProviderManager(kerberosServiceAuthenticationProvider,
				activeDirectoryLdapAuthenticationProvider);

		http
			.authorizeHttpRequests((authz) -> authz
				.requestMatchers("/", "/api/public").permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling((exceptions) -> exceptions
				.authenticationEntryPoint(spnegoEntryPoint())
			)
			.formLogin((form) -> form
				.loginPage("/login").permitAll()
			)
			.logout((logout) -> logout
				.permitAll()
			)
			.authenticationProvider(activeDirectoryLdapAuthenticationProvider())
			.authenticationProvider(kerberosServiceAuthenticationProvider())
			.addFilterBefore(spnegoAuthenticationProcessingFilter(providerManager),
				BasicAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
		return new ActiveDirectoryLdapAuthenticationProvider(adDomain, adServer);
	}

	@Bean
	public SpnegoEntryPoint spnegoEntryPoint() {
		return new SpnegoEntryPoint("/login");
	}

	public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
			AuthenticationManager authenticationManager) {
		SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
		filter.setAuthenticationManager(authenticationManager);
		return filter;
	}

	public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() throws Exception {
		KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
		provider.setTicketValidator(sunJaasKerberosTicketValidator());
		provider.setUserDetailsService(ldapUserDetailsService());
		return provider;
	}

	@Bean
	public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
		SunJaasKerberosTicketValidator ticketValidator = new SunJaasKerberosTicketValidator();
		ticketValidator.setServicePrincipal(servicePrincipal);
		ticketValidator.setKeyTabLocation(new FileSystemResource(keytabLocation));
		ticketValidator.setDebug(true);
		return ticketValidator;
	}

	@Bean
	public LdapContextSource ldapContextSource() {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl(adServer);
		// Don't set base here - FilterBasedLdapUserSearch will handle the search base
		contextSource.setUserDn("cn=admin,dc=example,dc=org");
		contextSource.setPassword("admin");
		contextSource.afterPropertiesSet();
		return contextSource;
	}

	public SunJaasKrb5LoginConfig loginConfig() throws Exception {
		SunJaasKrb5LoginConfig loginConfig = new SunJaasKrb5LoginConfig();
		loginConfig.setKeyTabLocation(new FileSystemResource(keytabLocation));
		loginConfig.setServicePrincipal(servicePrincipal);
		loginConfig.setDebug(true);
		loginConfig.setIsInitiator(true);
		loginConfig.afterPropertiesSet();
		return loginConfig;
	}

	@Bean
	public LdapUserDetailsService ldapUserDetailsService() throws Exception {
		LdapUserSearch userSearch = new KerberosAwareLdapUserSearch(ldapSearchBase, ldapSearchFilter, ldapContextSource());
		
		// Active Directory: Just pulls from memberOf attribute
		// LdapUserDetailsService service =
		// new LdapUserDetailsService(userSearch, new DefaultActiveDirectoryAuthoritiesPopulator());

		// Configure group search for member attribute
		DefaultLdapAuthoritiesPopulator authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(
			ldapContextSource(), 
			"dc=example,dc=org"  // Group search base
		);
		authoritiesPopulator.setGroupSearchFilter("(member={0})");  // Search groups where member={userDN}
		authoritiesPopulator.setGroupRoleAttribute("cn");  // Use cn as role name
		authoritiesPopulator.setSearchSubtree(true);  // Search subtree
		
		LdapUserDetailsService service = new LdapUserDetailsService(userSearch, authoritiesPopulator);
		service.setUserDetailsMapper(new LdapUserDetailsMapper());
		return service;
	}

	/**
	 * Custom LDAP user search that extracts username from Kerberos principal
	 * Converts "user123@EXAMPLE.ORG" to "user123" for LDAP search
	 */
	private class KerberosAwareLdapUserSearch implements LdapUserSearch {
		private final FilterBasedLdapUserSearch delegate;
		private final String searchBase;
		private final String searchFilter;

		public KerberosAwareLdapUserSearch(String searchBase, String searchFilter, LdapContextSource contextSource) {
			this.searchBase = searchBase;
			this.searchFilter = searchFilter;
			this.delegate = new FilterBasedLdapUserSearch(searchBase, searchFilter, contextSource);
		}

		@Override
		public DirContextOperations searchForUser(String username) {
			// Extract username from Kerberos principal (e.g., "user123@EXAMPLE.ORG" -> "user123")
			String actualUsername = username;
			if (username.contains("@")) {
				actualUsername = username.substring(0, username.indexOf("@"));
			}
			return delegate.searchForUser(actualUsername);
		}
	}
}