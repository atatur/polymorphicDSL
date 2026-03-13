parser grammar AllGrammarsTwoParser;

options {tokenVocab=AllGrammarsLexer; }

customStartRule : ALL_INPUTS+;
