package app.alegon.aws.sct.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AwsSctContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AwsSctContextListener.class);

    private final ApplicationArguments applicationArguments;

    public AwsSctContextListener(ApplicationArguments applicationArguments) {
        this.applicationArguments = applicationArguments;
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        List<String> args = applicationArguments.getOptionValues("mode");
        if (args != null && args.size() > 0) {
            logger.info("Running in mode: " + args.get(0));
            logger.info("<=====================================================>");
        }
    }
}