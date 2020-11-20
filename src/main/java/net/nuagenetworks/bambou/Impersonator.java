package net.nuagenetworks.bambou;


// Usage: Impersonator.impersonate("user1","org1",session, () -> object.fetch(session));

public class Impersonator {

    public static void impersonate(String username, String enterprise, RestSession session, ImpersonationCallback fn) throws Exception {
        session.setImpersonationUsername(username);
        session.setImpersonationEnterprise(enterprise);
        fn.run();
        session.setImpersonationUsername(null);
        session.setImpersonationEnterprise(null);
    }   
}

