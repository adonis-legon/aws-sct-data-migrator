package app.alegon.aws.sct.migrator.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BeanHelper {
    @Autowired
    private ApplicationContext applicationContext;

    public Object getBeanByName(String beanName) {
        return applicationContext.getBean(beanName);
    }
}
