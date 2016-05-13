package be.nabu.libs.services.pojo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.types.CollectionHandlerFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.Element;

public class MethodServiceInstance implements ServiceInstance {

	private MethodService definition;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	MethodServiceInstance(MethodService definition) {
		this.definition = definition;
	}
	
	@Override
	public MethodService getDefinition() {
		return definition;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		List<Object> arguments = new ArrayList<Object>();
		Class<?>[] parameterTypes = getDefinition().getMethod().getParameterTypes();
		Type[] genericParameterTypes = getDefinition().getMethod().getGenericParameterTypes();
		if (input != null) {
			int i = 0;
			for (Element<?> element : TypeUtils.getAllChildren(input.getType())) {
				Object value = input.get(element.getName());
				// do some conversion if necessary
				if (value != null) {
					// the following check was deprecated (2016-01-25) because it fails to detect lists of unmatched types
					// !parameterTypes[i].isAssignableFrom(value.getClass())
					CollectionHandlerProvider sourceCollectionHandler = CollectionHandlerFactory.getInstance().getHandler().getHandler(value.getClass());
					if (sourceCollectionHandler != null && !parameterTypes[i].equals(Object.class)) {
						CollectionHandlerProvider targetCollectionHandler = CollectionHandlerFactory.getInstance().getHandler().getHandler(parameterTypes[i]);
						if (targetCollectionHandler == null) {
							throw new ServiceException("POJO-1", "The source object '" + element.getName() + "' is a collection but the target object '" + parameterTypes[i] + "' is not");
						}
						Collection sourceIndexes = sourceCollectionHandler.getIndexes(value);
						Class<?> targetType = targetCollectionHandler.getComponentType(genericParameterTypes[i]);
						Object targetCollection = targetCollectionHandler.create(parameterTypes[i], sourceIndexes.size());
						for (Object index : sourceIndexes) {
							Object item = sourceCollectionHandler.get(value, index);
							if (item != null && !targetType.isAssignableFrom(item.getClass()) && item instanceof ComplexContent) {
								item = TypeUtils.getAsBean((ComplexContent) item, targetType);
							}
							targetCollectionHandler.set(targetCollection, index, item);
						}
						arguments.add(targetCollection);
					}
					else if (value instanceof ComplexContent) {
						Class<?> targetType = parameterTypes[i];
						if (Object.class.equals(targetType) || ComplexContent.class.isAssignableFrom(targetType)) {
							arguments.add(value);	
						}
						else {
							arguments.add(TypeUtils.getAsBean((ComplexContent) value, targetType));
						}
					}
					else {
						arguments.add(value);
					}
				}
				else {
					arguments.add(value);
				}
				i++;
			}
		}
		else {
			for (int i = 0; i < genericParameterTypes.length; i++) {
				arguments.add(null);
			}
		}
		try {
			Object instance = getDefinition().getSourceClass().newInstance();
			if (getDefinition().getInjectContext() != null) {
				getDefinition().getInjectContext().set(instance, executionContext);
			}
			if (getDefinition().getInjectRuntime() != null) {
				getDefinition().getInjectRuntime().set(instance, ServiceRuntime.getRuntime());
			}
			logger.debug("Invoking {} ({})", getDefinition().getMethod(), arguments);
			Object returnValue = getDefinition().getMethod().invoke(instance, arguments.toArray());
			ComplexContent response = getDefinition().getServiceInterface().getOutputDefinition().newInstance();
			if (returnValue != null) {
				response.set(
					getDefinition().getServiceInterface().getOutputDefinition().iterator().next().getName(),
					returnValue
				);
			}
			return response;
		}
		catch (Exception e) {
			throw new ServiceException(e);
		}
	}

}