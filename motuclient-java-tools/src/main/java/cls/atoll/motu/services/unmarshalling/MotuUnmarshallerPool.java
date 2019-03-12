package cls.atoll.motu.services.unmarshalling;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cls.atoll.motu.exceptions.MotuResponseParsingException;
import cls.atoll.motu.exceptions.MotuRuntimeException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Reader;

@Component(value = "motu-unmarshaller-pool")
public class MotuUnmarshallerPool extends GenericObjectPool<Unmarshaller> {
    @Autowired
    public MotuUnmarshallerPool(final MotuPooledUnmarshallerFactory factory) {
        super(factory);
    }

    @Override
    public Unmarshaller borrowObject() {
        try {
            return super.borrowObject();
        } catch (final Exception e) {
            throw new MotuRuntimeException(e);
        }
    }

    @Override
    public Unmarshaller borrowObject(final long borrowMaxWaitMillis) throws MotuRuntimeException {
        try {
            return super.borrowObject(borrowMaxWaitMillis);
        } catch (final Exception e) {
            throw new MotuRuntimeException(e);
        }
    }

    public <T> T unmarshal(final Reader reader, final Class<T> clazz) throws MotuResponseParsingException {
        final Unmarshaller unmarshaller = this.borrowObject();
        try {
            return clazz.cast(unmarshaller.unmarshal(reader));
        } catch (final JAXBException | ClassCastException ex) {
            throw new MotuResponseParsingException(ex);
        } finally {
            this.returnObject(unmarshaller);
        }
    }
}
