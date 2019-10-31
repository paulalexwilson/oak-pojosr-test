# oak-pojosr-test
Attempted to use POJO-SR with Apache Oak

### Issues

I've attempted to follow the configuration described in the oak-pojosr as best I can. 

The test hangs whilst waiting for configuration to complete, indicating that the OSGi repository configuration is bad/incomplete. 

I see the following error.

```
INFO  SecurityProviderRegistration:478 - Aborting: preconditions are not satisfied: Preconditions(preconditions = [org.apache.jackrabbit.oak.security.authentication.token.TokenConfigurat
ionImpl, org.apache.jackrabbit.oak.spi.security.user.action.DefaultAuthorizableActionProvider, org.apache.jackrabbit.oak.security.user.UserAuthenticationFactoryImpl, org.apache.jackrabbit.oak.security.autho
rization.AuthorizationConfigurationImpl, org.apache.jackrabbit.oak.security.principal.PrincipalConfigurationImpl, org.apache.jackrabbit.oak.security.authorization.restriction.RestrictionProviderImpl], candi
dates = [org.apache.jackrabbit.oak.spi.security.user.action.DefaultAuthorizableActionProvider, org.apache.jackrabbit.oak.security.authentication.token.TokenConfigurationImpl, org.apache.jackrabbit.oak.secur
ity.user.UserAuthenticationFactoryImpl, org.apache.jackrabbit.oak.security.principal.PrincipalConfigurationImpl, org.apache.jackrabbit.oak.security.authorization.restriction.RestrictionProviderImpl])
```

I've debugged the code and can see that for some reason, AuthorizationConfigurationImpl is not registered. 
I have tried manually registering this service and the SecurityProviderRegistration completes OK, however the repository
creation still hangs. 

Strangely - the `oak-pojosr` project compiles fine however I cannot see where mine differs from the setup provided. 
