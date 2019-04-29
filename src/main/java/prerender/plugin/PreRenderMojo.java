package prerender.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * Says "Hi" to the user.
 *
 */
@Mojo( name = "sayhi")
public class PreRenderMojo extends AbstractMojo
{
    /**
     * The greeting to display.
     */
    @Parameter( property = "sayhi.greeting", defaultValue = "Hello World!" )
    private String greeting;
	
    /**
     * The source directory.
     */
    @Parameter( property = "sayhi.sourceDirectory", defaultValue = "src/main/java" )
    private String mainJavaPath;
	
    /**
     * The markdown directory.
     */
    @Parameter( property = "sayhi.resourseDirectory", defaultValue = "src/main/resources" )
    private String mainResourcesPath;
	
    public void execute() throws MojoExecutionException
    {
        getLog().info( greeting );
        // BasicSample.main(null);
        TokenReplacingPostProcessor.main(new String[] {mainJavaPath, mainResourcesPath});
        // getLog().info( StatementsLinesExample.main(null));
    }
}
