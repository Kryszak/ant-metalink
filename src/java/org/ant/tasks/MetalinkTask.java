package org.ant.tasks;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class MetalinkTask extends Task {

    private static final String NAMESPACE_URI = "urn:ietf:params:xml:ns:metalink";

    private static final String METALINK = "metalink";

    private static final String YES = "yes";

    private static final String FILE = "file";

    private static final String NAME = "name";

    private static final String SIZE = "size";

    private static final String HASH = "hash";

    private static final String TYPE = "type";

    private static final String MD5 = "md5";

    private static final String EMPTY = "";

    private static final String URL = "url";

    private static final String ALGORITHM = "MD5";

    private Iterator fileset;

    private String file;

    private String url;

    private Document document;

    private File workingDir;

    public void addConfiguredFileset(FileSet fs) {
        this.fileset = fs.iterator();
        this.workingDir = fs.getDir();
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void setProject(Project project) {
        super.setProject(project);
    }

    @Override
    public void execute() {
        if (this.url == null) {
            this.url = this.getProject().getProperty("server.files.url");
        }
        try {
            createMetalinkFile();
        } catch (ParserConfigurationException | TransformerException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private void createMetalinkFile() throws ParserConfigurationException, TransformerException, IOException, NoSuchAlgorithmException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();

        document = builder.newDocument();
        Element mainRootElement = document.createElementNS(NAMESPACE_URI, METALINK);
        document.appendChild(mainRootElement);

        addFileElements(this.fileset, mainRootElement);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, YES);
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new FileOutputStream(this.file));
        transformer.transform(source, result);
    }

    private void addFileElements(Iterator fileset, Element mainRootElement) throws IOException, NoSuchAlgorithmException {
        while (fileset.hasNext()) {
            FileResource file = (FileResource) fileset.next();
            if (!file.isDirectory()) {
                mainRootElement.appendChild(getFileElement(file.getFile()));
            } else {
                addFileElements(file.getFile(), mainRootElement);
            }
        }
    }

    private void addFileElements(File file, Element mainRootElement) throws IOException, NoSuchAlgorithmException {
        if (!file.isDirectory()) {
            mainRootElement.appendChild(getFileElement(file));
        } else {
            addFileElements(file, mainRootElement);
        }
    }


    private Element getFileElement(File file) throws IOException, NoSuchAlgorithmException {
        Element fileNode = document.createElement(FILE);
        fileNode.setAttribute(NAME, file.getName());
        fileNode.appendChild(getFileProperty(SIZE, Long.toString(file.length())));
        fileNode.appendChild(getFileProperty(HASH, getMd5Hash(file)));
        fileNode.appendChild(
                getFileProperty(
                        URL, this.url + file.getAbsolutePath()
                                .replace(workingDir.getAbsolutePath() + File.separator, EMPTY).replace('\\', '/'))
        );
        return fileNode;
    }

    private Node getFileProperty(String name, String value) {
        Element node = document.createElement(name);
        if (name.equals(HASH)) {
            node.setAttribute(TYPE, MD5);
        }
        node.appendChild(document.createTextNode(value));
        return node;
    }

    private String getMd5Hash(File file) throws NoSuchAlgorithmException, IOException {
        byte[] digest = null;
        digest = MessageDigest.getInstance(ALGORITHM).digest(Files.readAllBytes(file.toPath()));
        return convertMd5BytesToString(digest);
    }

    private String convertMd5BytesToString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }


}