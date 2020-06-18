package it.istc.pst.platinum.framework.microkernel.annotation.cfg.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import it.istc.pst.platinum.framework.domain.knowledge.DomainKnowledgeType;

/**
 * Component Configuration annotation
 * 
 * @author anacleto
 *
 */
@Target({
	ElementType.TYPE,
	ElementType.CONSTRUCTOR
})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainKnowledgeConfiguration {
	
	DomainKnowledgeType knowledge();
}