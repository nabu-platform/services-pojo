package be.nabu.libs.services.pojo;

import javax.jws.WebParam;
import javax.jws.WebResult;

import junit.framework.TestCase;
import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedServiceResolver;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.api.ComplexContent;

public class TestPOJO extends TestCase {
	
	public void testService() throws ServiceException {
		DefinedServiceResolver resolver = DefinedServiceResolverFactory.getInstance().getResolver();
		Service service = resolver.resolve(Test.class.getName() + ".doSomething");
		ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
		input.set("a", "testing");
		input.set("b", "this");
		ComplexContent output = service.newInstance().execute(ServiceUtils.newExecutionContext(), input);
		assertEquals("testing this", output.get("result"));
	}
	
	public static class Test {
		@WebResult(name = "result")
		public String doSomething(@WebParam(name="a") String a, @WebParam(name="b") String b) {
			return a + " " + b;
		}
	}
}
