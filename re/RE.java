package re;

import java.util.*;

import javax.sound.midi.Sequence;

import fa.State;
import fa.nfa.NFA;
import fa.nfa.NFAState;

/**
 * <h1>RE</h1> The RE class is responsible for implementing the REInterface
 * This class parses a regular expression to return an equivalent NFA
 * 
 * @author Jeff Kahn
 * @author Jackson Edwards
 * @version 1.0
 * @since 2021-04-29
 */
public class RE implements REInterface {

	private String input; // The regular expression 
	private Set<Character> alphabet = new HashSet<Character>(); // Alphabet of NFA
	private int stateCount; // Used for name creation of new states

	public RE(String input) {
		this.input = input;
		alphabet.add('a'); // Know from project description that 'a' will be in alphabet
		alphabet.add('b'); // Know from project description that 'b' will be in alphabet
		stateCount = 0;
	}

	@Override
	public NFA getNFA() {
		return regex();
	}

	/**
	* regex eats | and returns term or union 
	*@return returns term if there isnt "|" 
	*/
	private NFA regex() {
		NFA term = term();

		term.addAbc(alphabet);
		if (more() && peek() == '|') {
			eat('|');
			return unionNFA(term, regex());

		} else {
			return term;
		}

	}

	/**
	 * peek returns the character at location 0 in input without consuming
	 * @return
	 */
	private char peek() {
		return input.charAt(0);
	}

	/**
	 * consumes next input, fails if not equal to c
	 * @param c - character to compare
	 */
	private void eat(char c) {
		if (peek() == c)
			this.input = this.input.substring(1);
		else
			throw new RuntimeException("Expected: " + c + "; got: " + peek());
	}

	/**
	 * next eats c and returns it
	 * @return c - next input
	 */
	private char next() {
		char c = peek();
		eat(c);
		return c;
	}

	/**
	 * more returns true if more input
	 * @return true if input length is greater than 0 
	 */
	private boolean more() {
		return input.length() > 0;
	}

	/**
	 * Concatenates terms using factor until the next input is a ')' or '|'
	 * @return NFA that has a sequence of factors
	 */
	private NFA term() {
		NFA factorNFA = new NFA();
		NFAState newState = new NFAState(makeName()); // create a new state
		factorNFA.addStartState(newState.getName()); // add the start state
		NFAState finalState = new NFAState(makeName()); // create a new final state
		factorNFA.addFinalState(finalState.getName()); // add the final state
		factorNFA.addTransition(factorNFA.getStartState().getName(), 'e', finalState.getName()); // create transition from start to final

		while (more() && peek() != ')' && peek() != '|') {
			NFA nextFactor = factor(); // get the next factor
			factorNFA = concatenate(factorNFA, nextFactor); // Concatenate the factors
		}

		return factorNFA;
	}

	/**
	 * Uses the closure rules for regular languages to capture the union functionality
	 * @param first - NFA to be unioned
	 * @param second - NFA to be unioned
	 * @return NFA that is a union of first and second
	 */
	private NFA unionNFA(NFA first, NFA second) {
		
		NFAState newStartState = new NFAState(makeName()); // make new start state

		first.addNFAStates(second.getStates()); // add the states from second to first
		
		//Add empty transitions from new states to start states
		first.addTransition(newStartState.getName(), 'e', first.getStartState().getName());
		first.addTransition(newStartState.getName(), 'e', second.getStartState().getName());
		
		first.addStartState(newStartState.getName());
		second.addStartState(newStartState.getName());

		return first;

	}

	/**
	 * concatenate looks at all final states in first then creates transitions on empty to the start state 
	 * of second and makes the state nonfinal.
	 * @param first - NFA to concatenate
	 * @param second - NFA to concatenate
	 * @return - first with the updated final and start states
	 */
	private NFA concatenate(NFA first, NFA second) {
		// Create empty transitions from final states of first to start states of second
		// and make final states of m1 non-final
		for (State finals : first.getFinalStates()) {
			NFAState stateFinal = (NFAState) finals;
			stateFinal.addTransition('e', (NFAState) second.getStartState());
			stateFinal.setNonFinal();
		}
		first.addNFAStates(second.getStates()); // add NFA states of second to first

		return first;

	}

	/**
	 * factor - if there is a * char, factor will eat and call repetition on baseNFA
	 * @return - baseNFA
	 */
	private NFA factor() {
		NFA baseNFA = base();
		while (more() && peek() == '*') {
			eat('*');
			baseNFA = repetition(baseNFA); // allow repetition for star operation on base
		}

		return baseNFA;
	}

	/**
	 * Repetition - for every final state in internal, add transition on empty to current state and startstate.
	 * @param internal - NFA to be checked
	 * @return
	 */
	public NFA repetition(NFA internal) {
		NFAState startState = (NFAState) internal.getStartState();
		// create empty transitions from final to start state for looping
		for (State state : internal.getFinalStates()) {
			internal.addTransition(state.getName(), 'e', startState.getName());
		}
		NFAState newStart = new NFAState(makeName());
		internal.addStartState(newStart.getName());
		internal.addFinalState(newStart.getName());
		internal.addTransition(newStart.getName(), 'e', startState.getName());

		return internal;
	}

	/**
	 * base - looks at next char, if '(' eat it and call regex, if ')' break and return r
	 * @return - NFA with single character functionality or regular expression
	 */
	private NFA base() {
		NFA r;
		switch (peek()) {
		case '(':
			eat('(');
			r = regex();
			eat(')');
			break;
		default:
			r = primitive(next());
		}
		return r;
	}

	/**
	 * primitive - NFA for 'a' 'b' or 'e'
	 * @param c
	 * @return
	 */
	private NFA primitive(char c) {
		NFA nfa = new NFA();
		if (c == 'e') { // case for if character is empty
			NFAState emptyState = new NFAState(makeName());
			nfa.addStartState(emptyState.getName());
			nfa.addFinalState(emptyState.getName());
		} else {
			// functionality for single character
			NFAState oneState = new NFAState(makeName());
			nfa.addStartState(oneState.getName());
			NFAState twoState = new NFAState(makeName());
			nfa.addFinalState(twoState.getName());
			nfa.addTransition(oneState.toString(), c, twoState.toString());
		}
		return nfa;
	}

	/**
	 * makeName - creates a string that is the current stateCount
	 * @return - returns the string stateName
	 */
	private String makeName() {
		String stateName = Integer.toString(stateCount);
		stateCount++;
		return stateName;
	}

}
