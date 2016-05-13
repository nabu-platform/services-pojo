package be.nabu.libs.services.pojo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ExecutionContextProvider;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;

public class POJOUtils {
	
	private static final class ServiceInvocationHandler<T> implements InvocationHandler {
		private final ExecutionContextProvider executionContextProvider;
		private final Principal principal;
		private final Class<T> javaInterface;
		private Service[] services;

		private ServiceInvocationHandler(Class<T> javaInterface, ExecutionContextProvider executionContextProvider, Principal principal, Service...services) {
			this.executionContextProvider = executionContextProvider;
			this.principal = principal;
			this.javaInterface = javaInterface;
			this.services = services;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			for (Service service : services) {
				if (service == null) {
					continue;
				}
				MethodServiceInterface iface = MethodServiceInterface.wrap(method);
				logger.debug("Checking service '" + service + "' for interface: " + iface);
				if (!isImplementation(service, iface)) {
					continue;
				}
				ComplexContent input = iface.getInputDefinition().newInstance();
				int i = 0;
				for (Element<?> element : iface.getInputDefinition()) {
					input.set(element.getName(), args[i++]);
				}
				
				if (service instanceof DefinedService) {
					logger.debug("Executing service '{}' using java interface '{}'", ((DefinedService) service).getId(), javaInterface.getName());
				}
				
				ExecutionContext context;
				if (ServiceRuntime.getRuntime() != null) {
					context = ServiceRuntime.getRuntime().getExecutionContext();
				}
				else if (executionContextProvider == null) {
					throw new IllegalArgumentException("There is no service context available and no context provider was passed along");
				}
				else {
					context = executionContextProvider.newExecutionContext(principal);
				}
				ServiceRuntime serviceRuntime = new ServiceRuntime(service, context);
				ComplexContent output = serviceRuntime.run(input);
				if (void.class.isAssignableFrom(method.getReturnType()) || Void.class.isAssignableFrom(method.getReturnType()) || output == null) {
					return null;
				}
				else {
					Object returnValue = output.get(iface.getOutputDefinition().iterator().next().getName());
					if (returnValue == null) {
						return null;
					}
					else if (returnValue instanceof ComplexContent && !method.getReturnType().isAssignableFrom(returnValue.getClass())) {
						return TypeUtils.getAsBean((ComplexContent) returnValue, method.getReturnType());
					}
					else {
						return returnValue;
					}
				}
			}
			throw new IllegalStateException("No service found that implements the method: " + method);
		}
	}

	private static Logger logger = LoggerFactory.getLogger(POJOUtils.class);
	
	public static <T> T newProxy(final Class<T> javaInterface, final Service service, final ExecutionContext fixedContext) {
		return newProxy(javaInterface, service, new ExecutionContextProvider() {
			@Override
			public ExecutionContext newExecutionContext(Principal principal) {
				return fixedContext;
			}
		}, null);
	}
	
	public static <T> T newProxy(final Class<T> javaInterface, final Service service) {
		return newProxy(javaInterface, service, null, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T newProxy(final Class<T> javaInterface, final ExecutionContextProvider executionContextProvider, final Principal principal, final Service...services) {
		return (T) Proxy.newProxyInstance(javaInterface.getClassLoader(), new Class [] { javaInterface }, new ServiceInvocationHandler<T>(javaInterface, executionContextProvider, principal, services));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T newProxy(final Class<T> javaInterface, final Service service, final ExecutionContextProvider executionContextProvider, final Principal principal) {
		return (T) Proxy.newProxyInstance(javaInterface.getClassLoader(), new Class [] { javaInterface }, new ServiceInvocationHandler<T>(javaInterface, executionContextProvider, principal, service));
	}
	
	public static boolean isImplementation(Service service, ServiceInterface iface) {
		ServiceInterface serviceInterface = service.getServiceInterface();
		while (serviceInterface != null && !serviceInterface.equals(iface)) {
			serviceInterface = serviceInterface.getParent();
		}
		return serviceInterface != null;
	}
}
