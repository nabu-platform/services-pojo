package be.nabu.libs.services.pojo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.validation.constraints.NotNull;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.types.CollectionHandlerFactory;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedTypeResolver;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ArrayCollectionHandlerProvider;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.types.properties.CollectionHandlerProviderProperty;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.simple.Bytes;
import be.nabu.libs.types.structure.Structure;

public class MethodServiceInterface implements DefinedServiceInterface {

	private static Map<Method, MethodServiceInterface> ifaces = new HashMap<Method, MethodServiceInterface>();
	
	public static MethodServiceInterface wrap(Class<?> clazz, String name) {
		for (Method method : clazz.getMethods()) {
			if (method.getName().equals(name)) {
				return wrap(method);
			}
		}
		throw new IllegalArgumentException("No method found with name '" + name + "' in class " + clazz.getName());
	}
	
	public static MethodServiceInterface wrap(DefinedTypeResolver typeResolver, Method method) {
		if (!ifaces.containsKey(method)) {
			synchronized(MethodServiceInterface.class) {
				if (!ifaces.containsKey(method)) {
					ifaces.put(method, new MethodServiceInterface(typeResolver, method));
				}
			}
		}
		return ifaces.get(method);
	}
	
	public static MethodServiceInterface wrap(Method method) {
		return wrap(DefinedTypeResolverFactory.getInstance().getResolver(), method);
	}
	
	private Method method;
	private ComplexType input, output;
	private DefinedTypeResolver definedTypeResolver;
	private ServiceInterface parent;
	
	public MethodServiceInterface(DefinedTypeResolver definedTypeResolver, Method method) {
		this.definedTypeResolver = definedTypeResolver;
		this.method = method;
		ifaces: for (Class<?> iface : method.getDeclaringClass().getInterfaces()) {
			for (Method ifaceMethod : iface.getMethods()) {
				if (ifaceMethod.getName().equals(method.getName()) && Arrays.equals(ifaceMethod.getParameters(), method.getParameters())) {
					this.parent = MethodServiceInterface.wrap(definedTypeResolver, ifaceMethod);
					break ifaces;
				}
			}
		}
	}
	
	@Override
	public ComplexType getInputDefinition() {
		if (input == null) {
			synchronized(this) {
				if (input == null) {
					Structure structure = new Structure();
					structure.setName(getName(method));
					structure.setNamespace(getNamespace(method));
					structure.setProperty(new ValueImpl<Boolean>(new ElementQualifiedDefaultProperty(), true));
					Class<?>[] parameterTypes = method.getParameterTypes();
					java.lang.reflect.Type[] genericParameterTypes = method.getGenericParameterTypes();
					Annotation[][] parameterAnnotations = method.getParameterAnnotations();
					for (int i = 0; i < parameterTypes.length; i++) {
						boolean nullable = true; 
						String name = null;
						// try to deduce the name from a @WebParam annotation
						for (int j = 0; j < parameterAnnotations[i].length; j++) {
							if (parameterAnnotations[i][j] instanceof WebParam) {
								name = ((WebParam) parameterAnnotations[i][j]).name();
							}
							if (parameterAnnotations[i][j] instanceof NotNull) {
								nullable = false;
							}
						}
						if (name == null) {
							name = "arg" + i;
						}
						addElement(structure, name, parameterTypes[i], genericParameterTypes[i], new ValueImpl<Integer>(MinOccursProperty.getInstance(), nullable && !parameterTypes[i].isPrimitive() ? 0 : 1));
					}
					input = structure;
				}
			}
		}
		return input;
	}
	
	/**
	 * Should reuse the beantype logic for parameter detection based on bean validation!
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addElement(Structure parent, String name, Class<?> clazz, java.lang.reflect.Type genericType, Value<?>...possibleValues) {
		if (genericType == null) {
			genericType = clazz;
		}
		CollectionHandlerProvider<?, ?> handler = clazz.isArray() && !clazz.equals(byte[].class) ? new ArrayCollectionHandlerProvider((Class<? extends Object[]>) clazz) : CollectionHandlerFactory.getInstance().getHandler().getHandler(clazz);
		Class<?> actualType = handler == null ? clazz : handler.getComponentType(genericType);
		List<Value<?>> values = new ArrayList<Value<?>>(Arrays.asList(possibleValues));
		// always set minoccurs, by default all values are nullable and can be "optional" as long as they aren't primitive
		if (!ValueUtils.contains(MinOccursProperty.getInstance(), possibleValues) && !actualType.isPrimitive()) {
			values.add(new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0));
		}
		// if we have a collection handler, add a max/min occurs and the handler we know at this point
		if (handler != null) {
			values.add(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0));
			values.add(new ValueImpl<CollectionHandlerProvider>(new CollectionHandlerProviderProperty(), handler));
		}
		Type type = byte[].class.equals(actualType) ? new Bytes() : definedTypeResolver.resolve(TypeUtils.box(actualType).getName());
		if (type == null) {
			type = BeanResolver.getInstance().resolve(actualType);
			if (type == null) {
				throw new IllegalArgumentException("You are referencing a class that can not be resolved: " + clazz.getName());
			}
		}
		if (type instanceof SimpleType) {
			parent.add(new SimpleElementImpl(name, (SimpleType<?>) type, parent, values.toArray(new Value<?>[0])));
		}
		else {
			parent.add(new ComplexElementImpl(name, (ComplexType) type, parent, values.toArray(new Value<?>[0])));
		}
	}
	
	private static String getName(Method method) {
		return method.getAnnotation(WebMethod.class) != null
			? method.getAnnotation(WebMethod.class).operationName()
			: method.getName();
	}
	
	private static String getNamespace(Method method) {
		return method.getDeclaringClass().getAnnotation(WebService.class) != null
			? method.getDeclaringClass().getAnnotation(WebService.class).targetNamespace()
			: method.getDeclaringClass().getName();
	}

	@Override
	public ComplexType getOutputDefinition() {
		if (output == null) {
			synchronized(this) {
				if (output == null) {
					Structure structure = new Structure();
					structure.setName(getName(method) + "Response");
					structure.setNamespace(method.getAnnotation(WebResult.class) != null
							? method.getAnnotation(WebResult.class).targetNamespace()
							: getNamespace(method));
					structure.setProperty(new ValueImpl<Boolean>(new ElementQualifiedDefaultProperty(), true));
					String name = method.getAnnotation(WebResult.class) != null
						? method.getAnnotation(WebResult.class).name()
						: "response";
					if (!void.class.isAssignableFrom(method.getReturnType())) {
						addElement(structure, name, method.getReturnType(), method.getGenericReturnType());
					}
					output = structure;
				}
			}
		}
		return output;
	}

	@Override
	public String getId() {
		return method.getDeclaringClass().getName() + "." + method.getName();
	}

	@Override
	public ServiceInterface getParent() {
		return parent;
	}
	
	@Override
	public boolean equals(Object object) {
		return object instanceof MethodServiceInterface && ((MethodServiceInterface) object).method.equals(method);
	}
	
	@Override
	public int hashCode() {
		return method.hashCode();
	}
	
	@Override
	public String toString() {
		return method.getDeclaringClass().getName() + "." + method.getName() + "() [" + method.getDeclaringClass().getClassLoader() + "]";
	}
}
