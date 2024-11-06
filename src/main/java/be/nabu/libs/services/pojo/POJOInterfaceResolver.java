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
