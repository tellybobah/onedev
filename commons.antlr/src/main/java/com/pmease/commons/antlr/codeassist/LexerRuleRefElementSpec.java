package com.pmease.commons.antlr.codeassist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class LexerRuleRefElementSpec extends TokenElementSpec {

	private static final long serialVersionUID = 1L;

	private final String ruleName;
	
	private transient Optional<RuleSpec> rule;
	
	public LexerRuleRefElementSpec(CodeAssist codeAssist, String label, Multiplicity multiplicity, 
			int tokenType, String ruleName) {
		super(codeAssist, label, multiplicity, tokenType);

		this.ruleName = ruleName;
	}

	public String getRuleName() {
		return ruleName;
	}
	
	public RuleSpec getRule() {
		if (rule == null)
			rule = Optional.fromNullable(codeAssist.getRule(ruleName));
		return rule.orNull();
	}

	@Override
	public List<ElementSuggestion> doSuggestFirst(Node parent, ParseTree parseTree, 
			String matchWith, Set<String> checkedRules) {
		if (!checkedRules.contains(ruleName) && getRule() != null) {
			checkedRules.add(ruleName);
			return getRule().suggestFirst(parseTree, new Node(this, parent, null), matchWith, checkedRules);
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public MandatoryScan scanMandatories(Set<String> checkedRules) {
		if (!checkedRules.contains(ruleName) && getRule() != null) {
			checkedRules.add(ruleName);
		
			List<AlternativeSpec> alternatives = getRule().getAlternatives();
			if (alternatives.size() == 1) {
				List<String> mandatories = new ArrayList<>();
				for (ElementSpec elementSpec: alternatives.get(0).getElements()) {
					if (elementSpec.getMultiplicity() == Multiplicity.ZERO_OR_ONE 
							|| elementSpec.getMultiplicity() == Multiplicity.ZERO_OR_MORE) {
						return new MandatoryScan(mandatories, true);
					} else if (elementSpec.getMultiplicity() == Multiplicity.ONE_OR_MORE) {
						MandatoryScan scan = elementSpec.scanMandatories(new HashSet<>(checkedRules));
						mandatories.addAll(scan.getMandatories());
						return new MandatoryScan(mandatories, true);
					} else {
						MandatoryScan scan = elementSpec.scanMandatories(new HashSet<>(checkedRules));
						mandatories.addAll(scan.getMandatories());
						if (scan.isStop())
							return new MandatoryScan(mandatories, true);
					}
				}
				return new MandatoryScan(mandatories, false);
			} else {
				return MandatoryScan.stop();
			}
		} else {
			return MandatoryScan.stop();
		}
	}

	@Override
	protected boolean matchOnce(AssistStream stream, Map<String, Integer> checkedIndexes) {
		if (stream.isEof()) {
			return false;
		} else if (stream.getCurrentToken().getType() == type) {
			stream.increaseIndex();
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected List<TokenNode> getPartialMatchesOnce(AssistStream stream, 
			Node parent, Node previous, Map<String, Integer> checkedIndexes) {
		Preconditions.checkArgument(!stream.isEof());
		
		Token token = stream.getCurrentToken();
		if (token.getType() == type) {
			stream.increaseIndex();
			return Lists.newArrayList(new TokenNode(this, parent, previous, token));
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public String toString() {
		return "lexer_rule_ref: " + ruleName;
	}
	
}