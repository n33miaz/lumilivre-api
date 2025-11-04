package br.com.lumilivre.api.utils;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class AssignedIdentityGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {

        Serializable id = (Serializable) session.getEntityPersister(null, object).getIdentifier(object, session);

        if (id != null) {
            return id;
        }

        return null;
    }
}