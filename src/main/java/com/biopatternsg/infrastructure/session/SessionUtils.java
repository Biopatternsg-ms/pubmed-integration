package com.biopatternsg.infrastructure.session;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@RequestScoped
public class SessionUtils {

    private MultivaluedMap<String, String> context;

    public String getUserId(){
        return this.context.get("x-user-id").getFirst();
    }

}
