//package build.pluto.executor;
//
//import java.io.File;
//import java.io.Serializable;
//import java.lang.reflect.Field;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import build.pluto.builder.Builder;
//import build.pluto.builder.BuilderFactory;
//import build.pluto.dependency.Origin;
//import build.pluto.output.Output;
//
//public class ReflectiveBuilder<In extends Serializable, Out extends Output> 
//	extends Builder<ReflectiveBuilder.Input, Out> {
//
//	private static ClassLoader loader = ClassLoader.getSystemClassLoader();
//	private static Set<URL> loaded = new HashSet<>();
//	
//	public static class Factory<In extends Serializable, Out extends Output> 
//		implements BuilderFactory<Input, Out, ReflectiveBuilder<In, Out>> {
//
//		private static final long serialVersionUID = 519382945250902332L;
//
//		@Override
//		public ReflectiveBuilder<In, Out> makeBuilder(Input input) {
//			return new ReflectiveBuilder<>(input);
//		}
//
//		@Override
//		public boolean isOverlappingGeneratedFileCompatible(File overlap,
//				Serializable input, BuilderFactory<?, ?, ?> otherFactory,
//				Serializable otherInput) {
//			return false;
//		}
//		
//	}
//	
//	public static <In extends Serializable, Out extends Output> 
//			BuilderFactory<Input, Out, ReflectiveBuilder<In, Out>> factory() {
//		return new Factory<>();
//	}
//	
//	public static class Input implements Serializable {
//		private static final long serialVersionUID = 6368484213073178045L;
//		
//		public final String builderClass;
//		public final Object builderInput;
//		public final Origin classOrigin;
//		public final List<File> classPath;
//		
//		public Input(String builderClass, Object builderInput, Origin classOrigin, List<File> classPath) {
//			this.builderClass = builderClass;
//			this.builderInput = builderInput;
//			this.classOrigin = classOrigin;
//			this.classPath = classPath;
//		}
//	}
//	
//	public ReflectiveBuilder(Input input) {
//		super(input);
//	}
//
//	@Override
//	protected String description(Input input) {
//		return null;
//	}
//
//	@Override
//	public File persistentPath(Input input) {
//		return null;
//	}
//
//	
//	@SuppressWarnings("unchecked")
//	private ExecutableBuilderFactory<In, Out, Builder<In, Out>> loadBuilderFactory(Input input) throws ClassNotFoundException {
//		
//		Class<Builder<In, Out>> builderClass = (Class<Builder<In, Out>>) loader.loadClass(input.builderClass);
//		
//		// check for field 'factory' in builderClass 
//		try {
//			Field factoryField = builderClass.getField("factory");
//			if (factoryField != null && ExecutableBuilderFactory.class.isAssignableFrom(factoryField.getType()))
//				return (ExecutableBuilderFactory<In, Out, Builder<In, Out>>) factoryField.get(null);
//		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
//			// ignore
//		}
//
//		// check for method 'factory()' in builderClass 
//		try {
//			Method factoryMethod = builderClass.getMethod("factory");
//			if (factoryMethod != null)
//				return (ExecutableBuilderFactory<In, Out, Builder<In, Out>>) factoryMethod.invoke(null);
//		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
//			// ignore
//		}
//
//		throw new IllegalArgumentException("Could not load factory for builder " + input.builderClass);
//	}
//	
//	private void setupClassLoader(List<File> classPath) throws MalformedURLException {
//		List<URL> urls = new ArrayList<>();
//		for (File file : classPath) {
//			URL url = file.toURI().toURL();
//			if (loaded.add(url))
//				urls.add(url);
//		}
//		loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), loader);
//	}
//
//	@Override
//	protected Out build(Input input) throws Throwable {
//		requireBuild(input.classOrigin);
//		setupClassLoader(input.classPath);
//		
//		ExecutableBuilderFactory<In, Out, Builder<In,Out>> factory = loadBuilderFactory(input);
//		InputParser<In> parser = factory.inputParser();
//		
//		In in = parser.parse(input.builderInput);
//		return requireBuild(factory, in);
//	}
//}
