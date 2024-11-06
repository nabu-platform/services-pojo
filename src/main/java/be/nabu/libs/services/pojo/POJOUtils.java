/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.libs.services.pojo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ExecutionContextProvider;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.api.ServiceResult;
import be.nabu.libs.services.api.ServiceRunner;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.ListCollectionHandlerProvider;

public class POJOUtils {
	
	public static final class ServiceInvocationHandler<T> implements InvocationHandler {
		private final ExecutionContextProvider executionContextProvider;
		private final Token token;
		private final Class<T> javaInterface;
		private Service[] services;
		
		// you can set an explicit service runner for e.g. remote execution
		private ServiceRunner runner;

		private ServiceInvocationHandler(Class<T> javaInterface, ExecutionContextProvider executionContextProvider, Token token, Service...services) {
			this(javaInterface, executionContextProvider, token, null, services);
		}
		
		private ServiceInvocationHandler(Class<T> javaInterface, ExecutionContextProvider executionContextProvider, Token token, ServiceRunner runner, Service...services) {
			this.executionContextProvider = executionContextProvider;
			this.token = token;
			this.javaInterface = javaInterface;
			this.runner = runner;
			this.services = services;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
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
					context = executionContextProvider.newExecutionContext(token);
				}
				ComplexContent output;
				if (runner != null) {
					Future<ServiceResult> run = runner.run(service, context, input);
					output = run.get().getOutput();
				}
				else {
					ServiceRuntime serviceRuntime = new ServiceRuntime(service, context);
					output = serviceRuntime.run(input);
				}
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
					else if (returnValue instanceof Collection) {
						Class<?> componentType = new ListCollectionHandlerProvider().getComponentType(method.getGenericReturnType());
						List list = new ArrayList();
						for (Object child : (Collection) returnValue) {
							list.add(child instanceof ComplexContent ? TypeUtils.getAsBean((ComplexContent) child, componentType) : child);
						}
						return list;
					}
					else {
						return returnValue;
					}
				}
			}
			if (method.isDefault()) {
				Method invokeDefault = getInvokeDefault();
				if (invokeDefault != null) {
					return invokeDefault.invoke(null, proxy, method, args);
				}
				// we don't do the java 8 workaround because it requires reflection that is illegal from java 9 onwards
				// we generally run 11 LTS anyway, so this should work
				// once we switch to 17 LTS, the invokedefault should kick in (to be tested)
				else {
					for (Class<?> iface : proxy.getClass().getInterfaces()) {
						MethodHandle special = MethodHandles.lookup().findSpecial(
                    		iface, 
                    		method.getName(), 
                    		MethodType.methodType(method.getReturnType(), method.getParameterTypes()),  
                			iface
                		);
						if (special != null) {
							return special.bindTo(proxy).invokeWithArguments(args);
						}
					}

				}
				throw new IllegalStateException("No service found that implements the default method: " + method);
			}
			throw new IllegalStateException("No service found that implements the method: " + method);
		}

		public Token getToken() {
			return token;
		}

		public Class<T> getJavaInterface() {
			return javaInterface;
		}

		public Service[] getServices() {
			return services;
		}
		
	}

	private static Logger logger = LoggerFactory.getLogger(POJOUtils.class);
	
	// the java 16 invoke default method (if found)
	private static Method invokeDefault;
	private static boolean invokeDefaultResolved;
	
	private static Method getInvokeDefault() { 
		if (!invokeDefaultResolved) {
			try {
				// from java 16 onwards, you can use this approach to call default methods
				// we can't yet mandate java 16 at the time of writing so we do this via reflection
				Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass("java.lang.reflect.InvocationHandler");
				for (Method potentialMethod : clazz.getMethods()) {
					if (potentialMethod.getName().equals("invokeDefault")) {
						invokeDefault = potentialMethod;
					}
				}
			}
			catch (Exception e) {
				// ignore
			}
			finally {
				invokeDefaultResolved = true;
			}
		}
		return invokeDefault;
	}
	
	public static <T> T newProxy(final Class<T> javaInterface, final Service service, final ExecutionContext fixedContext) {
		return newProxy(javaInterface, service, new ExecutionContextProvider() {
			@Override
			public ExecutionContext newExecutionContext(Token primary, Token...alternatives) {
				return fixedContext;
			}
		}, null);
	}
	
	public static <T> T newProxy(final Class<T> javaInterface, final Service service) {
		return newProxy(javaInterface, service, null, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T newProxy(final Class<T> javaInterface, final ExecutionContextProvider executionContextProvider, final Token token, final Service...services) {
		return (T) Proxy.newProxyInstance(javaInterface.getClassLoader(), new Class [] { javaInterface }, new ServiceInvocationHandler<T>(javaInterface, executionContextProvider, token, services));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T newProxy(final Class<T> javaInterface, final ExecutionContextProvider executionContextProvider, final Token token, final ServiceRunner runner, final Service...services) {
		return (T) Proxy.newProxyInstance(javaInterface.getClassLoader(), new Class [] { javaInterface }, new ServiceInvocationHandler<T>(javaInterface, executionContextProvider, token, runner, services));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T newProxy(final Class<T> javaInterface, final Service service, final ExecutionContextProvider executionContextProvider, final Token token) {
		return (T) Proxy.newProxyInstance(javaInterface.getClassLoader(), new Class [] { javaInterface }, new ServiceInvocationHandler<T>(javaInterface, executionContextProvider, token, service));
	}
	
	public static boolean isImplementation(Service service, ServiceInterface iface) {
		ServiceInterface serviceInterface = service.getServiceInterface();
		while (serviceInterface != null && !serviceInterface.equals(iface)) {
			serviceInterface = serviceInterface.getParent();
		}
		return serviceInterface != null;
	}
}
