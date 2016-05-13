package be.nabu.libs.services.pojo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;

public class POJOResolver implements DefinedServiceResolver {

	@Override
	public DefinedService resolve(String id) {
		// the last part of the id is the method
		int index = id.lastIndexOf('.');
		if (index >= 0) {
			String className = id.substring(0, index);
			String methodName = id.substring(index + 1);
			try {
				Class<?> clazz = Class.forName(className);
				for (Method method : clazz.getDeclaredMethods()) {
					if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && method.getName().equals(methodName)) {
						return new MethodService(DefinedTypeResolverFactory.getInstance().getResolver(), clazz, method);
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
