package cls.motu.services.unmarshalling;

import fr.cls.atoll.motu.api.message.xml.RequestSize;
import fr.cls.atoll.motu.api.message.xml.StatusModeResponse;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

@Component
public class MotuPooledUnmarshallerFactory implements PooledObjectFactory<Unmarshaller> {

    private static volatile JAXBContext jaxbContext = null;

    private static JAXBContext getContext() throws JAXBException {
        if (null == jaxbContext) {
            synchronized (MotuPooledUnmarshallerFactory.class) {
                if (null == jaxbContext) {
                    jaxbContext = JAXBContext.newInstance(StatusModeResponse.class, RequestSize.class);
                }
            }
        }

        return jaxbContext;
    }

    @Override
    public PooledObject<Unmarshaller> makeObject() throws JAXBException {
        final JAXBContext context = getContext();
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return new DefaultPooledObject<>(unmarshaller);
    }

    @Override
    public void destroyObject(final PooledObject<Unmarshaller> pooledObject) {
        // do nothing
    }

    @Override
    public boolean validateObject(final PooledObject<Unmarshaller> pooledObject) {
        return true;
    }

    @Override
    public void activateObject(final PooledObject<Unmarshaller> pooledObject) {
        // do nothing
    }

    @Override
    public void passivateObject(final PooledObject<Unmarshaller> pooledObject) {
        // do nothing
    }
}
