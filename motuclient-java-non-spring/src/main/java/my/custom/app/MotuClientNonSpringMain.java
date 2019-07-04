package my.custom.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import my.custom.app.example.MotuClientNonSpringApplication;

public class MotuClientNonSpringMain {

    private static final Logger log = LoggerFactory.getLogger(MotuClientNonSpringApplication.class);

    /**
     * .
     * 
     * @param args
     */
    public static void main(String[] args) {
        log.debug("Start Motuclient Java");
        try (AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-context.xml")) {
            MotuClientNonSpringApplication mca = applicationContext.getBean(MotuClientNonSpringApplication.class);
            mca.run(args);
        }
        log.debug("End Motuclient Java");
    }
}
