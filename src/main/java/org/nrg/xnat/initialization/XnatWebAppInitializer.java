package org.nrg.xnat.initialization;

import org.apache.axis.transport.http.AdminServlet;
import org.apache.axis.transport.http.AxisHTTPSessionListener;
import org.apache.axis.transport.http.AxisServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.Turbine;
import org.nrg.framework.processors.XnatModuleBean;
import org.nrg.xdat.servlet.XDATAjaxServlet;
import org.nrg.xdat.servlet.XDATServlet;
import org.nrg.xnat.restlet.servlet.XNATRestletServlet;
import org.nrg.xnat.restlet.util.UpdateExpirationCookie;
import org.nrg.xnat.security.XnatSessionEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.*;
import java.io.IOException;
import java.util.*;

public class XnatWebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    public void onStartup(final ServletContext context) throws ServletException {
        context.setInitParameter("org.restlet.component", "org.nrg.xnat.restlet.XNATComponent");

        // If the context path is not empty (meaning this isn't the root application), then we'll get true: Restlet will
        // autowire its calls. If the context path is empty (meaning that this is the root application), autowire will
        // be false.
        context.setInitParameter("org.restlet.autoWire", Boolean.toString(StringUtils.isNotEmpty(context.getContextPath())));

        // Initialize the Spring stuff.
        super.onStartup(context);

        // Now initialize everything else.
        context.addFilter("springSecurityFilterChain", DelegatingFilterProxy.class).addMappingForUrlPatterns(null, false, "/*");
        context.addFilter("updateExpirationCookie", UpdateExpirationCookie.class).addMappingForUrlPatterns(null, false, "/*");

        context.addListener(XnatSessionEventPublisher.class);
        context.addListener(AxisHTTPSessionListener.class);

        Turbine.setTurbineServletConfig(new XnatTurbineConfig(context));

        _context = context;

        addServlet(XDATServlet.class, 1, "/xdat/*");
        addServlet(Turbine.class, 2, "/app/*");
        addServlet(XNATRestletServlet.class, 2, "/REST/*", "/data/*");
        addServlet(XDATAjaxServlet.class, 3, "/ajax/*", "/servlet/XDATAjaxServlet", "/servlet/AjaxServlet");
        addServlet(AxisServlet.class, 4, "/servlet/AxisServlet", "*.jws", "/services/*");
        addServlet(AdminServlet.class, 5, "/servlet/AdminServlet");
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/admin/*", "/xapi/*" };
    }

    @Override
    protected Class<?>[] getRootConfigClasses() {
        final List<Class<?>> configClasses = new ArrayList<>();
        configClasses.add(RootConfig.class);
        configClasses.addAll(getModuleConfigs());
        return configClasses.toArray(new Class[configClasses.size()]);
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[0];
    }

    private List<Class<?>> getModuleConfigs() {
        final List<Class<?>> moduleConfigs = new ArrayList<>();
        try {
            final PathMatchingResourcePatternResolver resolver  = new PathMatchingResourcePatternResolver();
            final Resource[]                          resources = resolver.getResources("classpath*:META-INF/xnat/**/*-module.properties");
            for (final Resource resource : resources) {
                final Properties     properties   = PropertiesLoaderUtils.loadProperties(resource);
                final XnatModuleBean module       = new XnatModuleBean(properties);
                final Class<?>       moduleConfig = module.getConfigClass();
                moduleConfigs.add(moduleConfig);
            }
        } catch (IOException e) {
            throw new RuntimeException("An error occurred trying to locate XNAT module definitions.");
        } catch (ClassNotFoundException e) {
            _log.error("Did not find a class specified in a module definition.", e);
        }

        return moduleConfigs;
    }

    private void addServlet(final Class<? extends Servlet> clazz, final int loadOnStartup, final String... mappings) {
        final String                      name = StringUtils.uncapitalize(clazz.getSimpleName());
        final ServletRegistration.Dynamic registration  = _context.addServlet(name, clazz);
        registration.setLoadOnStartup(loadOnStartup);
        registration.addMapping(mappings);
    }

    private static class XnatTurbineConfig implements ServletConfig {
        public XnatTurbineConfig(final ServletContext context) {
            _context = context;
        }

        @Override
        public String getServletName() {
            return "Turbine";
        }

        @Override
        public ServletContext getServletContext() {
            return _context;
        }

        @Override
        public String getInitParameter(final String s) {
            if (s.equals("properties")) {
                return "WEB-INF/conf/TurbineResources.properties";
            }
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            final List<String> parameters = new ArrayList<>();
            parameters.add("properties");
            return Collections.enumeration(parameters);
        }

        private ServletContext _context;
    }

    private static final Logger _log = LoggerFactory.getLogger(XnatWebAppInitializer.class);
    private ServletContext _context;
}
