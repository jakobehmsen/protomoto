package protomoto.patterns;

import java.util.Map;
import protomoto.ast.ASTCell;
import protomoto.ast.ASTCellVisitor;

public class Patterns {
    public static Pattern none() {
        return new Pattern() {
            @Override
            public boolean matches(ASTCell cell, Map<String, ASTCell> captures) {
                return false;
            }
        };
    }
    
    public static Pattern any() {
        return new Pattern() {
            @Override
            public boolean matches(ASTCell cell, Map<String, ASTCell> captures) {
                return true;
            }
        };
    }
    
    public static Pattern capture(Pattern pattern, String name) {
        return new Pattern() {
            @Override
            public boolean matches(ASTCell cell, Map<String, ASTCell> captures) {
                if(pattern.matches(cell, captures)) {
                    captures.put(name, cell);
                    return true;
                }
                
                return false;
            }
        };
    }
    
    public static Pattern equalsString(String stringToMatch) {
        return new Pattern() {
            @Override
            public boolean matches(ASTCell cell, Map<String, ASTCell> captures) {
                return cell.accept(new ASTCellVisitor<Boolean>() {
                    @Override
                    public Boolean visitList(ASTCell[] items) {
                        return false;
                    }

                    @Override
                    public Boolean visitString(String string) {
                        return string.equals(stringToMatch);
                    }

                    @Override
                    public Boolean visitInteger(int i) {
                        return false;
                    }
                });
            }
        };
    }
    
    private static ListStream listStream(ASTCell[] items, int start) {
        return new ListStream() {
            private int index = start;

            @Override
            public ASTCell peek() {
                return items[index];
            }

            @Override
            public void consume() {
                index++;
            }

            @Override
            public int remaining() {
                return items.length - index;
            }

            @Override
            public ListStreamPosition position() {
                return new ListStreamPosition() {
                    private int start = index;

                    @Override
                    public ASTCell[] consumed() {
                        int count = index - start;
                        ASTCell[] consumed = new ASTCell[count];
                        System.arraycopy(items, start, consumed, 0, count);
                        return consumed;
                    }
                };
            }

            @Override
            public ListStream sublist() {
                return listStream(items, index);
            }
        };
    }
    
    public static Pattern subsumesList(ListPattern listPattern) {
        return new Pattern() {
            @Override
            public boolean matches(ASTCell cell, Map<String, ASTCell> captures) {
                return cell.accept(new ASTCellVisitor<Boolean>() {
                    @Override
                    public Boolean visitList(ASTCell[] items) {
                        ListStream listStream = listStream(items, 0);
                        
                        return listPattern.matches(listStream, captures, null);
                    }

                    @Override
                    public Boolean visitString(String string) {
                        return false;
                    }

                    @Override
                    public Boolean visitInteger(int i) {
                        return false;
                    }
                });
            }
        };
    }
}
