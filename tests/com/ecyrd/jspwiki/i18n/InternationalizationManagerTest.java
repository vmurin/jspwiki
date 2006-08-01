package com.ecyrd.jspwiki.i18n;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;

import com.ecyrd.jspwiki.TestEngine;

public class InternationalizationManagerTest extends TestCase
{
    TestEngine engine;
    InternationalizationManager mgr;
    
    public static final String TEST_BUNDLE = "test";
    
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            PropertyConfigurator.configure(props);

            engine = new TestEngine( props );
            mgr = new InternationalizationManager( engine );
        }
        catch( IOException e ) {}
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testSimpleGet()
    {
        Locale loc = Locale.GERMAN;
        
        String res = mgr.get(TEST_BUNDLE, loc, "hello" );
        
        assertEquals( "hallo", res );
    }
}
