package be.nabu.libs.services.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceDescription;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.api.DefinedTypeResolver;

public class MethodService implements DefinedService {

	private Class<?> clazz;
	private Method method;
	private ServiceInterface serviceInterface;
	private DefinedTypeResolver definedTypeResolver;
	private String description;
	private boolean descriptionResolved;
	/**
	 * You can have the service runtime injected into your bean, this is the reflected field where it will be put
	 */
	private Field injectContext, injectRuntime;
	
	public MethodService(DefinedTypeResolver definedTypeResolver, Class<?> clazz, Method method) {
		this.definedTypeResolver = definedTypeResolver;
		this.clazz = clazz;
		this.method = method;
		this.injectContext = getInject(clazz, ExecutionContext.class);
		this.injectRuntime = getInject(clazz, ServiceRuntime.class);
	}
	
	private Field getInject(Class<?> clazz, Class<?> clazzToFind) {
		for (Field field : clazz.getDeclaredFields()) {
			if (field.getType().equals(clazzToFind)) {
				if (!field.isAccessible()) {
					field.setAccessible(true);
				}
				return field;
			}
		}
		return null;
	}
	
	Field getInjectContext() {
		return injectContext;
	}
	
	Field getInjectRuntime() {
		return injectRuntime;
	}

	@Override
	public MethodServiceInstance newInstance() {
		return new MethodServiceInstance(this);
	}

	public Method getMethod() {
		return method;
	}
	
	public Class<?> getSourceClass() {
		return clazz;
	}

	@Override
	public ServiceInterface getServiceInterface() {
		if (serviceInterface == null) {
			synchronized(this) {
				if (serviceInterface == null) {
					serviceInterface = MethodServiceInterface.wrap(definedTypeResolver, method);
				}
			}
		}
		return serviceInterface;
	}

	@Override
	public Set<String> getReferences() {
		return new HashSet<String>();
	}

	@Override
	public String getId() {
		return getSourceClass().getName() + "." + getMethod().getName();
	}
	
	@Override
	public String toString() {
		return "MethodService:" + getId();
	}
	
	@Override
	public String getDescription() {
		if (!descriptionResolved) {
			descriptionResolved = true;
			ServiceDescription annotation = getMethod().getAnnotation(ServiceDescription.class);
			if (annotation != null) {
				description = annotation.description();
			}
		}
		return description;
	}
}
