package kth.id2209.homework3.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import kth.id2209.homework1.agent.Utilities;

import java.io.IOException;
import java.util.Arrays;

import static jade.lang.acl.MessageTemplate.MatchPerformative;

/**
 * Created by tharidu on 11/23/16.
 */
public class Queen extends Agent {
    boolean firstQueen;
    boolean findAll;
    int[] board;
    int boardSize;
    int column;
    int lastProposed = -1;
    AID previousQueen = null;
    AID nextQueen = null;

    private static final String STATE_A = "A";
    private static final String STATE_B = "B";
    private static final String STATE_C = "C";
    private static final String STATE_D = "D";

    protected void setup() {
        boardSize = (int) getArguments()[0];
        column = (int) getArguments()[1];

        // First queen ?
        if (getArguments().length == 5) {
            board = (int[]) getArguments()[2];
            firstQueen = (boolean) getArguments()[3];
            findAll = (boolean) getArguments()[4];
        } else {
            int prevQ = column - 1;
            previousQueen = new AID("queen" + prevQ, AID.ISLOCALNAME);
        }

        if (column != boardSize - 1) {
            int nextQ = column + 1;
            nextQueen = new AID("queen" + nextQ, AID.ISLOCALNAME);
        }

        System.out.println("Hello! " + getAID().getName() + " is ready. Previous queen is " + (previousQueen == null ?
                "" : previousQueen.getLocalName()) + " next queen is " + (nextQueen == null ? "" : nextQueen.getLocalName()));

        if (firstQueen) {
            FSMBehaviour firstQueenFSM = new FSMBehaviour();

            firstQueenFSM.registerFirstState(new OneShotBehaviour() {
                int availablePositions = 1;

                @Override
                public void action() {
                    availablePositions = 1;
                    try {
                        // Tried all
                        if (lastProposed + 1 == boardSize) {
                            availablePositions = 0;
                        } else {
                            board[column] = ++lastProposed;
                            ACLMessage aclMessage = Utilities.createAclMessage(ACLMessage.PROPOSE, new AID[]{nextQueen}, board);
                            myAgent.send(aclMessage);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                public int onEnd() {
                    return availablePositions;
                }
            }, STATE_A);

            firstQueenFSM.registerState(new SimpleBehaviour() {
                boolean replyReceived;
                int performativeReceived;

                @Override
                public void action() {
                    replyReceived = false;
                    ACLMessage aclMessage = myAgent.receive(MessageTemplate.or(MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MatchPerformative(ACLMessage.REJECT_PROPOSAL)));
                    if (aclMessage != null) {
                        performativeReceived = aclMessage.getPerformative();
                        if (aclMessage.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            try {
                                int[] recvBoard = (int[]) aclMessage.getContentObject();
                                System.out.println(Arrays.toString(recvBoard));
                                PrintBoard(recvBoard);
                            } catch (UnreadableException e) {
                                e.printStackTrace();
                            }
                        }

                        replyReceived = true;
                    } else {
                        block();
                    }
                }

                @Override
                public boolean done() {
                    return replyReceived;
                }

                public int onEnd() {
                    return performativeReceived;
                }
            }, STATE_B);

            firstQueenFSM.registerLastState(new OneShotBehaviour() {
                @Override
                public void action() {
                    System.out.println("Queen" + column + " is quitting");
                }
            }, STATE_C);

            firstQueenFSM.registerTransition(STATE_A, STATE_B, 1);
            firstQueenFSM.registerTransition(STATE_A, STATE_C, 0); // No available positions to try

            firstQueenFSM.registerTransition(STATE_B, STATE_A, ACLMessage.REJECT_PROPOSAL, new String[]{STATE_A, STATE_B});

            if (findAll) {
                firstQueenFSM.registerTransition(STATE_B, STATE_A, ACLMessage.ACCEPT_PROPOSAL, new String[]{STATE_A, STATE_B});
            } else {
                firstQueenFSM.registerTransition(STATE_B, STATE_C, ACLMessage.ACCEPT_PROPOSAL);
            }

            addBehaviour(firstQueenFSM);
        } else {
            FSMBehaviour fsm = new FSMBehaviour();

            fsm.registerFirstState(new SimpleBehaviour() {
                boolean msgReceived = false;

                @Override
                public void action() {
                    msgReceived = false;
                    ACLMessage aclMessage = myAgent.receive(MatchPerformative(ACLMessage.PROPOSE));
                    if (aclMessage != null) {
                        try {
                            board = (int[]) aclMessage.getContentObject();
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }

                        msgReceived = true;
                    } else {
                        block();
                    }
                }

                @Override
                public boolean done() {
                    return msgReceived;
                }
            }, STATE_A);

            fsm.registerState(new OneShotBehaviour() {
                int availablePositions = 1;

                @Override
                public void action() {
                    try {
                        int row = -1;
                        availablePositions = 1;

                        // Rows
                        for (int i = ++lastProposed; i < boardSize; i++) {
                            if (IsValidPosition(i, column)) {
                                row = i;
                                lastProposed = row;
                                break;
                            }
                        }

                        // No valid position
                        if (row == -1) {
                            availablePositions = 0;
                            lastProposed = -1;

                            ACLMessage aclMessage = Utilities.createAclMessage(ACLMessage.REJECT_PROPOSAL, new AID[]{previousQueen}, board);
                            myAgent.send(aclMessage);
                        } else {
                            board[column] = lastProposed;

                            ACLMessage aclMessage;
                            if (nextQueen == null) {
                                // Last queen - found solution
                                aclMessage = Utilities.createAclMessage(ACLMessage.ACCEPT_PROPOSAL, new AID[]{previousQueen}, board);
                                availablePositions = -1;
                            } else {
                                aclMessage = Utilities.createAclMessage(ACLMessage.PROPOSE, new AID[]{nextQueen}, board);
                            }

                            myAgent.send(aclMessage);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                public int onEnd() {
                    return availablePositions;
                }
            }, STATE_B);

            fsm.registerState(new SimpleBehaviour() {
                boolean replyReceived;
                int performativeReceived;

                @Override
                public void action() {
                    replyReceived = false;
                    ACLMessage aclMessage = myAgent.receive(MessageTemplate.or(MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MatchPerformative(ACLMessage.REJECT_PROPOSAL)));
                    if (aclMessage != null) {
                        performativeReceived = aclMessage.getPerformative();
                        if (aclMessage.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            try {
                                ACLMessage replyMessage = Utilities.createAclMessage(ACLMessage.ACCEPT_PROPOSAL, new AID[]{previousQueen}, aclMessage.getContentObject());
                                myAgent.send(replyMessage);
                            } catch (UnreadableException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        replyReceived = true;
                    } else {
                        block();
                    }
                }

                @Override
                public boolean done() {
                    return replyReceived;
                }

                public int onEnd() {
                    return performativeReceived;
                }
            }, STATE_C);
            fsm.registerLastState(new OneShotBehaviour() {
                @Override
                public void action() {
                    System.out.println("No more solutions");
                }
            }, STATE_D);

            fsm.registerDefaultTransition(STATE_A, STATE_B);
            fsm.registerTransition(STATE_B, STATE_C, 1); // Send PROPOSE
            fsm.registerTransition(STATE_B, STATE_A, -1, new String[]{STATE_A, STATE_B, STATE_C}); // Last queen - found solution
            fsm.registerTransition(STATE_B, STATE_A, 0, new String[]{STATE_A, STATE_B}); // No positions - send REJECT_PROPOSAL
            fsm.registerTransition(STATE_C, STATE_A, ACLMessage.ACCEPT_PROPOSAL, new String[]{STATE_A, STATE_B, STATE_C}); // Send ACCEPT_PROPOSAL to previous queen
            fsm.registerTransition(STATE_C, STATE_B, ACLMessage.REJECT_PROPOSAL, new String[]{STATE_A, STATE_B, STATE_C}); // Got REJECT_PROPOSAL - goto STATE B

            addBehaviour(fsm);
        }
    }

    private boolean IsValidPosition(int row, int column) {
        if (column == 0)
            return true;

        // Check rows
        for (int i = 0; i < column; i++) {
            if (board[i] == row)
                return false;
        }

        // Check upper diagonal
        for (int i = row - 1, j = column - 1; i >= 0 && j >= 0; i--, j--) {
            if (board[j] == i) {
                return false;
            }
        }

        // Check lower diagonal
        for (int i = row + 1, j = column - 1; i < board.length && j >= 0; i++, j--) {
            if (board[j] == i) {
                return false;
            }
        }

        return true;
    }

    private void PrintBoard(int[] board) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                if (board[j] == i)
                    System.out.print(" Q ");
                else
                    System.out.print(" * ");
            }
            System.out.println();
        }
    }
}
