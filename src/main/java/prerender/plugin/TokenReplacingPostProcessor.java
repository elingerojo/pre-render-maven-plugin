package prerender.plugin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithIdentifier;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.base.Strings;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory;
import com.vladsch.flexmark.util.NodeTracker;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import com.vladsch.flexmark.util.sequence.SubSequence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Stack;

/**
 * A sample that demonstrates how to strip (replace) specific tokens from a parsed
 * {@link Document} prior to rendering.
 */
public class TokenReplacingPostProcessor {

    static final MutableDataSet OPTIONS = new MutableDataSet();
    static {
        OPTIONS
        .set(Parser.EXTENSIONS, Arrays.asList(ReferenceReplacingExtension.create()))
        .set(HtmlRenderer.INDENT_SIZE, 2);
    }

    static final Parser PARSER = Parser.builder(OPTIONS).build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();
    
    static String mainJavaPath;

    static class ReferenceReplacingPostProcessor extends NodePostProcessor {

        static class Factory extends NodePostProcessorFactory {

            public Factory(DataHolder options) {
                super(false);

                // addNodes(Link.class);
                // addNodes(Image.class);
                addNodes(Reference .class);
            }

            @Override
            public NodePostProcessor create(Document document) {
                return new ReferenceReplacingPostProcessor();
            }
        }

        @Override
        public void process(NodeTracker state, Node node) {
        	/*
            if (node instanceof Link) { // [foo](http://example.com)
                Link link = (Link) node;
                Text text = new Text(link.getText());
                link.insertAfter(text);
                state.nodeAdded(text);
                
                System.out.println("Text: " + link.getText());
                System.out.println("URL: " + link.getUrl());
                System.out.println("Title: " + link.getTitle());

                link.unlink();
                state.nodeRemoved(link);
            } else
            */
        	
        	String referenceJavaCodeKeyword = "@code"; // Case-insensitive
      		Optional<com.github.javaparser.ast.Node> foundNode;
        	
        	if (node instanceof Reference) { // [reference]: URL text 
                Reference reference = (Reference) node;
                Text text = new Text(reference.getReference());
                String keyword = text.getChars().toString();
                
                
                // Skip node that does not contain the Java code keyword 
                if ( keyword.toLowerCase().contains(referenceJavaCodeKeyword)) {
                    // reference.insertAfter(text);
                    // state.nodeAdded(text);
                	
                	String filePath = reference.getUrl().toString();
                	String rawSelector = reference.getTitle().toString();
                	
                	// Extract id selector for id atribute (used to show/hide collapse code)
                	String idSelector = "";
                	String[] preRawSelectors = rawSelector.split("#");
                	if (preRawSelectors.length > 1) {
                		// There is an id attribute. Let's extract it
                		idSelector = preRawSelectors[0];
                		rawSelector = preRawSelectors[1];
                	}
                	
                	// Default to zero, meaning: Show all lines of snippet code found
                	int showLinesQty = 0;
            		
             		// Extract first charater from rawSelector when a digit is available
            		if (Character.isDigit(rawSelector.charAt(0))) {
            			showLinesQty = Character.getNumericValue(rawSelector.charAt(0));
            			rawSelector = rawSelector.substring(1);
            		}

                    System.out.println("filePath: " + mainJavaPath + reference.getUrl());
                    System.out.println("linePath: " + reference.getTitle());

                    System.out.println();
                    foundNode = parseJavaCode(filePath, rawSelector);
                    if (foundNode.isPresent()) {
                    	System.out.println("----------------------------------------");
                    	System.out.println("----------------------------------------");
                    	System.out.println(foundNode.get().getParentNodeForChildren().toString());
                    	System.out.println("----------------------------------------");
                    	System.out.println("----------------------------------------");
                    	
                    	String codeSnippet = foundNode.get().getParentNodeForChildren().toString();
 
                    	/*
                    	 * Extract a limited number of code lines
                    	 * (zero means: No limit, show all lines)
                    	 */
                    	if (showLinesQty != 0) {
                        	String[] lines = codeSnippet.split("\\r?\\n");
                        	codeSnippet = "";
                        	// Guard against trying to show more line that available
                        	int minLines = Math.min(lines.length - 1, showLinesQty);
                        	for (int i=0; i<minLines; i++) {
                        		codeSnippet = codeSnippet + lines[i] + System.lineSeparator();
                        	}
                        	// Add ellipsis, line separators ...and last line
                        	codeSnippet = codeSnippet + System.lineSeparator() +
                        			"// more code..." + System.lineSeparator() + System.lineSeparator() +
                        			lines[lines.length - 1] + System.lineSeparator();
                    	}
                    	
                    	CharSequence charSequence = codeSnippet;
                    	BasedSequence basedSequence = CharSubSequence.of(charSequence);
                    	List<BasedSequence> lineSegments = new ArrayList<>();
                    	lineSegments.add(basedSequence);
                    	
                        FencedCodeBlock codeBlock = new FencedCodeBlock();
                        codeBlock.setContent(lineSegments);
                        
                    	charSequence = "```";
                    	basedSequence = CharSubSequence.of(charSequence);
                    	codeBlock.setOpeningMarker(basedSequence);
                    	codeBlock.setClosingMarker(basedSequence);
                    	
                    	charSequence = "java";
                    	basedSequence = CharSubSequence.of(charSequence);
                    	codeBlock.setInfo(basedSequence);

                        System.out.println("codeBlock: " + codeBlock.toAstString(true));
                    	System.out.println("----------------------------------------");
                    	
                    	/*
                    	 * Wrap in a collapsable `DIV` with an id attribute
                    	 */
                    	HtmlBlock htmlBlockOpenDiv = new HtmlBlock();
                    	String divClassAndId = idSelector == "" ? "<div>" : "<div class=\"my-collapse\" id=\"" +
                    			idSelector + "\">";
                    	charSequence = divClassAndId;
                    	basedSequence = CharSubSequence.of(charSequence);
                    	List<BasedSequence> openingLineSegments = new ArrayList<>();
                    	openingLineSegments.add(basedSequence);
                    	htmlBlockOpenDiv.setContent(openingLineSegments);;

                    	HtmlBlock htmlBlockClosingDiv = new HtmlBlock();
                    	String readMoreAndClosingDiv = idSelector == "" ? "</div>" :
                    		"<p class=\"read-more pos-abs\"><button class=\"btn btn-danger\" type=\"button\" data-toggle=\"collapse\" data-target=\"#" +
                    		idSelector + "\" aria-expanded=\"false\" aria-controls=\"collapseExample\">Show/Hide code</button></p></div>";
                    	charSequence = readMoreAndClosingDiv;
                    	basedSequence = CharSubSequence.of(charSequence);
                    	List<BasedSequence> closingLineSegments = new ArrayList<>();
                    	closingLineSegments.add(basedSequence);
                    	htmlBlockClosingDiv.setContent(closingLineSegments);;
                    	
                        reference.insertAfter(htmlBlockClosingDiv);
                    	state.nodeAdded(htmlBlockClosingDiv);
                        
                        reference.insertAfter(codeBlock);
                        state.nodeAdded(codeBlock);
                        
                        reference.insertAfter(htmlBlockOpenDiv);
                        state.nodeAdded(htmlBlockOpenDiv);
                        
                    }
                    System.out.println();
                    
                    // reference.unlink();
                    // state.nodeRemoved(reference);
                }
            }
        }
    }

    /**
     * An extension that registers a post processor which intentionally strips (replaces)
     * specific link and image-link tokens after parsing.
     */
    static class ReferenceReplacingExtension implements Parser.ParserExtension {

        private ReferenceReplacingExtension() { }

        @Override
        public void parserOptions(MutableDataHolder options) { }

        @Override
        public void extend(Parser.Builder parserBuilder) {
            parserBuilder.postProcessorFactory(new ReferenceReplacingPostProcessor.Factory(parserBuilder));
        }

        public static Extension create() {
            return new ReferenceReplacingExtension();
        }
    }
    
    public static Optional<com.github.javaparser.ast.Node> parseJavaCode(String filePath, String rawSelector) {
   		Optional<com.github.javaparser.ast.Node> foundNode;
   		Optional<com.github.javaparser.ast.Node> previousFirstInLineNode;
		foundNode = Optional.empty();
		previousFirstInLineNode = Optional.empty();

		try {
        	File javaFile = new File(mainJavaPath + filePath);
        	JavaParser jParser = new JavaParser();
        	Optional<CompilationUnit> cu = jParser.parse(javaFile).getResult();
        	if (cu.isPresent()) {
        		System.out.println("cu.isPresent(): " + cu.isPresent());
        		String[] selectors = rawSelector.split(">");
        		// TODO: Guard against selectors empty
        		int selectorIndex = 0;
        		int lastLineNumber = 0;
        		int previousFirstLineNumber = 0;
        		int traversalNodeLineNumber;
        		
        		System.out.println("Selectors: " + selectors.toString());
        		
        		String actualSelector = selectors[selectorIndex];
        		
        		/*
        		int siblingCounter = 1;
        		
        		// Extract sibling counter digit when available
        		if (Character.isDigit(selectors[selectorIndex].charAt(0))) {
        			siblingCounter = Character.getNumericValue(selectors[selectorIndex].charAt(0));
        			actualSelector = selectors[selectorIndex].substring(1);
        		}
        		*/
        		
          		boolean isFound = false;
        		com.github.javaparser.ast.Node actualNode = null;
        		
           		boolean isSearchEnded = false;
        		actualNode = cu.get();
        		
        		do {
        			System.out.println("New Iterator for: " + actualNode.getBegin().get().line);
        			// BreadthFirstIterator bfIterator = new BreadthFirstIterator(actualNode);
        			PreOrderIterator bfIterator = new PreOrderIterator(actualNode);
        			
        			com.github.javaparser.ast.Node traversalNode;
        			
        			System.out.println("First do - actualSelector: " + actualSelector);
        			
        			do {
        				do {
            				traversalNode = bfIterator.next();
            				traversalNodeLineNumber = traversalNode.getBegin().get().line;
            				System.out.println("TraversalNodeLineNumber: " + traversalNodeLineNumber);
            				if (traversalNodeLineNumber != previousFirstLineNumber) {
            					previousFirstInLineNode = Optional.of(traversalNode);
            					previousFirstLineNumber = traversalNodeLineNumber;
            				}
        				} while (bfIterator.hasNext() && traversalNodeLineNumber <= lastLineNumber);
        				
        				foundNode = Optional.empty();
        				if (traversalNodeLineNumber > lastLineNumber) {
            				
                			System.out.print("  Second do - traversalNode: " + traversalNode.getMetaModel());
                			System.out.println(" with hash: " + traversalNode.hashCode());
            				/*
                			if (traversalNode instanceof MethodDeclaration &&
                            		((MethodDeclaration) traversalNode).getNameAsString().equals(actualSelector)) {
                				foundNode =  Optional.of(traversalNode);
                			} else if (traversalNode instanceof ConstructorDeclaration   &&
                					((ConstructorDeclaration) traversalNode).getNameAsString().equals(actualSelector)) {
                				foundNode =  Optional.of(traversalNode);
                			} else */ 
            				if (traversalNode instanceof Statement) {
            					String firstUppercasedSelector = actualSelector.substring(0, 1).toUpperCase() +
            							actualSelector.substring(1);
            					String metaModel = ((Statement) traversalNode).getMetaModel().toString();
            					System.out.println("metaModel: " + metaModel + " - firstUppercasedSelector: " + firstUppercasedSelector);
            					if (metaModel.equals("IfStmt") && firstUppercasedSelector.equals("Else")) {
            						System.out.println("ElseStmt found with else selector - " + ((Statement ) traversalNode).getMetaModel().toString());
            						IfStmt ifStmtNode = (IfStmt) traversalNode;
            						Optional<Statement> optElseNode = ifStmtNode.getElseStmt();
            						if (optElseNode.isPresent()) {
            							// Else node is present but needs conversion from Optional<Statment> to Optional<Node>
            							foundNode = Optional.of(optElseNode.get());
            						}
            					} else if(metaModel.equals(firstUppercasedSelector + "Stmt")) {
            						System.out.println(((Statement ) traversalNode).getMetaModel().toString());
            						System.out.println("TraversaNode.begin: " + traversalNode.getBegin().get().line);
            						foundNode =  Optional.of(traversalNode);
            					}
            				} else if (traversalNode instanceof VariableDeclarator) {
                    			System.out.println("getNameAsString(): " + ((VariableDeclarator) traversalNode).getNameAsString());
                				if (((VariableDeclarator) traversalNode).getNameAsString().equals(actualSelector)) {
	                				foundNode =  Optional.of(traversalNode);
	                			}
                			} else if (traversalNode instanceof NodeWithIdentifier) {
                      			System.out.println("getIdentifier(): " + ((NodeWithIdentifier) traversalNode).getIdentifier());
                				if (((NodeWithIdentifier) traversalNode).getIdentifier().equals(actualSelector)) {
                					foundNode =  Optional.of(traversalNode);
                				}
                			} else if (traversalNode instanceof NodeWithSimpleName) {
                      			System.out.println("getNameAsString(): " + ((NodeWithSimpleName) traversalNode).getNameAsString());
                      	                				if (((NodeWithSimpleName) traversalNode).getNameAsString().equals(actualSelector)) {
                					foundNode =  Optional.of(traversalNode);
                				}
                			} else if (traversalNode instanceof NodeWithName) {
                      			System.out.println("getNameAsString(): " + ((NodeWithName) traversalNode).getNameAsString());
                				if (((NodeWithName) traversalNode).getNameAsString().equals(actualSelector)) {
                					foundNode =  Optional.of(traversalNode);
                				}
                			};
        				}
 
        			
        			} while (bfIterator.hasNext() && !foundNode.isPresent());
    				
    				if (foundNode.isPresent()) {
    					actualNode = foundNode.get();
    					
        				lastLineNumber = traversalNodeLineNumber;
    					
    					selectorIndex++;
    					if (selectorIndex >= selectors.length) {
    						// No more selectors waiting. Means, node has been found.
    						isFound = true;
    	                    System.out.println("actualNode: [Lines " + actualNode.getBegin().get().line
    	                            + " - " + actualNode.getEnd().get().line + " ]");
    	                    foundNode = previousFirstInLineNode;
    	                    actualNode = foundNode.get();
    	                    System.out.println("previousFirstInLineNode: [Lines " + actualNode.getBegin().get().line
    	                            + " - " + actualNode.getEnd().get().line + " ]");
    					} else {
    						// There are more selectors waiting
    		        		actualSelector = selectors[selectorIndex];
    					}
    				} else {
    					isSearchEnded = true;
    	                System.out.println("Selectors not found in source java file - No more nodes to search");
    				}
        		} while (!isFound && !isSearchEnded);

        	}
        }
        catch (IOException e) {
            new RuntimeException(e);
        }
		return foundNode;
    }
    
    /**
     * Performs a breadth-first node traversal starting with a given node.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Breadth-first_search">Breadth-first traversal</a>
     */
    public static class BreadthFirstIterator implements Iterator<com.github.javaparser.ast.Node>{

        private final Queue<com.github.javaparser.ast.Node> queue = new LinkedList<>();

        public BreadthFirstIterator(com.github.javaparser.ast.Node node) {
            queue.add(node);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public com.github.javaparser.ast.Node next() {
        	com.github.javaparser.ast.Node next = queue.remove();
        	System.out.println("Iterator - next(): " + next.getChildNodes().size() + " children added to queue");
            queue.addAll(next.getChildNodes());
            return next;
        }
    }

    /**
     * Performs a pre-order (or depth-first) node traversal starting with a given node.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Pre-order">Pre-order traversal</a>
     */
    public static class PreOrderIterator implements Iterator<com.github.javaparser.ast.Node> {

        private final Stack<com.github.javaparser.ast.Node> stack = new Stack<>();

        public PreOrderIterator(com.github.javaparser.ast.Node node) {
            stack.add(node);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public com.github.javaparser.ast.Node next() {
        	com.github.javaparser.ast.Node next = stack.pop();
            List<com.github.javaparser.ast.Node> children = next.getChildNodes();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.add(children.get(i));
            }
            return next;
        }
    }

    public static void main(String[] args) {
    	mainJavaPath = args[0];
    	String mainResourcesPath = args[1];
    	String markdownsPath = mainResourcesPath + "/templates/markdowns";
    	String templatesPath = mainResourcesPath + "/templates/generated";
    	File markdownsDir = new File(markdownsPath);
    	File templatesDir = new File(templatesPath);

        new DirExplorer((level, path, file) -> path.endsWith(".md"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            
            // Remove extension (including the dot)
            String htmlFilename = path.substring(0, path.length() - 3);
            
            FileWriter fr = null;
            File archivo = null;
            
            try {
            	String fileContent = new String(Files.readAllBytes(Paths.get(markdownsPath + path))); 
            	
                System.out.println("Contienido: " + fileContent);

                Node document = PARSER.parse(fileContent);
                String html = RENDERER.render(document);

                // System.out.println(html);
                archivo = new File(Paths.get(templatesDir + htmlFilename + ".html").toString());
                fr = new FileWriter(archivo, false);
                
                String tagEnclosedHtml = "<div>\r\n" + html + "\r\n</div>";
                
                fr.write(tagEnclosedHtml);
                fr.close();
                
            	
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }).explore(markdownsDir);

        
        
        
    }
}