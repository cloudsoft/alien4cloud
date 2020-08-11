package alien4cloud.tosca.parser;

import alien4cloud.tosca.parser.mapping.generator.MappingGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Test tosca parsing for Tosca Simple profile in YAML wd03
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
public class MappingGeneratorTest {
    @Resource
    private MappingGenerator mappingGenerator;

    @Test
    public void test() throws ParsingException {
        mappingGenerator.process("tosca-simple-profile-wd03-mapping.yml");
    }
}