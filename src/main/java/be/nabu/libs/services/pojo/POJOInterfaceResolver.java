package be.nabu.libs.services.pojo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;

public class POJOInterfaceResolver implements DefinedServiceInterfaceResolver {

	private ClassLoader loader;

	public POJOInterfaceResolver(ClassLoader loader) {
		this.loader = loader;
	}
	
	public POJOInterfaceResolver() {
		// empty constructor
	}
	
	@Override
	public DefinedServiceInterface resolve(String id) {
		// the last part of the id is the method
		int index = id.lastIndexOf('.');
		if (index >= 0) {
			String className = id.substring(0, index);
			String methodName = id.substring(index + 1);
			try {
				Class<?> clazz = loader == null ? Thread.currentThread().getContextClassLoader().loadClass(className) : loader.loadClass(className);
				if (clazz.isInterface()) {
					for (Method method : clazz.getDeclaredMethods()) {
						if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && method.getName().equals(methodName)) {
							return MethodServiceInterface.wrap(DefinedTypeResolverFactory.getInstance().getResolver(), method);
						}
					}
				}
			}
			// it's not a valid class, just return null, maybe someone else can resolve it
			catch (ClassNotFoundException e) {
				// do nothing
			}
		}
		return null;
	}

}
