package nars.gui.input;

import automenta.vivisect.swing.NWindow;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/**
<<<<<<< HEAD
=======
/**
>>>>>>> origin/graphplan1
 *
 * @author me
 */
public class NarseseTemplatePanel {

    
    abstract static class TemplateElement {        }
    static class Budget extends TemplateElement {        }
    static class Text extends TemplateElement {
        public final String value;

        public Text(String value) {
            this.value = value;
        }
        @Override public String toString() {
            return value.trim();
        }

    }
    static class Concept extends TemplateElement {        
        String id;

        public Concept(String id) {
            this.id = id;
        }

        @Override public String toString() {
            return '#' + id;
        }
        
    }
    static class Truth extends TemplateElement {        }
    
    public static class NarseseTemplate {
        
        public final Map<String,Concept> concepts = new HashMap();
        public final Map<String,List<TemplateElement>> forms = new HashMap();

        public NarseseTemplate(String narsese, String english) {
            forms.put("narsese", parse(narsese));
            forms.put("en", parse(english));
        }

        public List<TemplateElement> parse(String p) /*throws InvalidTemplateException*/ {
            
            Pattern P = Pattern.compile("~t|~b|~\\#[\\D]|[.]+");
            
                    
            Matcher m = P.matcher(p);
            
            List<String> pieces = new ArrayList();
            int i = 0;
            while (m.find()) {
                if (m.start() > i) {
                    pieces.add(p.substring(i, m.start()));
                }
                pieces.add(m.group());
                i = m.end();
            }
            if (p.length() > i) {
                pieces.add(p.substring(i, p.length()));
            }
            
            List<TemplateElement> l = new LinkedList();
            for (String a : pieces) {
                if (a.equals("~t"))
                    l.add(new Truth());
                else if (a.equals("~b"))
                    l.add(new Budget());
                else if (a.startsWith("~#"))
                    l.add(new Concept(a.substring(2)));
                else
                    l.add(new Text(a));
            }
            System.out.println(l);

            //TODO check for unbalanced variables
            
            return l;
        }

        
        @Override
        public String toString() {
            return getSummaryString("en");
        }

        private String getSummaryString(String form) {
            StringBuilder s = new StringBuilder();
            for (TemplateElement e : forms.get(form)) {
                if (!form.equals("narsese") && (!(e instanceof Text) || (e instanceof Concept)))
                    continue;
                s.append(e.toString()).append(" ");    
            }
            return s.toString();
        }
        
        
        
        
        
    }
    
    public static JPanel newPanel(NarseseTemplate t, String form) {
        JPanel p = new JPanel(new FlowLayout());
        
        List<TemplateElement> l = t.forms.get(form);
        for (TemplateElement e : l) {
            if (e instanceof Text) {
                Text text = (Text)e;
                p.add(new JLabel(text.value));
            }
            else if (e instanceof Concept) {
                p.add(new JComboBox());
            }
            else if (e instanceof Truth) {
                p.add(new JLabel("[truth]"));
            }
            else if (e instanceof Budget) {
                p.add(new JLabel("[budget]"));                
            }
        }
        
        return p;
    }
    
    
    public static JPanel newPanel(final NarseseTemplate t) {
        final JPanel p = new JPanel(new BorderLayout());
        
        final JComboBox formSelect = new JComboBox();
        p.add(formSelect, BorderLayout.WEST);
        
        for (String f : t.forms.keySet())
            formSelect.addItem(f);
        
        ActionListener change = new ActionListener() {
            JPanel r = null;
            @Override public void actionPerformed(ActionEvent e) {
                if (r!=null) {
                    p.remove(r);                    
                }

                r = newPanel(t, formSelect.getSelectedItem().toString());
                
                p.add(r, BorderLayout.CENTER);           
                p.validate();
            }            
        };
        
        formSelect.addActionListener(change);
        
        change.actionPerformed(null);
        
        return p;
    }

    private static JPanel newPanel(List<NarseseTemplate> templates) {
        JPanel p = new JPanel(new BorderLayout());
        
        JPanel menu = new JPanel(new BorderLayout());
        DefaultMutableTreeNode tree = new DefaultMutableTreeNode();
        
        for (NarseseTemplate n : templates) {
            DefaultMutableTreeNode nt = new DefaultMutableTreeNode(n);
            tree.add(nt);
        }
        JTree t = new JTree(tree);
        menu.add(t, BorderLayout.CENTER);
        
        final JComboBox formSelect = new JComboBox();
        formSelect.addItem("en");
        formSelect.addItem("narsese");
        menu.add(formSelect, BorderLayout.NORTH);
        
        
        p.add(menu, BorderLayout.WEST);
        
        ActionListener change = new ActionListener() {
            JPanel r = null;
            @Override public void actionPerformed(ActionEvent e) {
                if (t.getSelectionModel().isSelectionEmpty()) return;
                Object o = 
                        ((DefaultMutableTreeNode)t.getSelectionModel().getSelectionPath().getLastPathComponent()).getUserObject();
                
                if (!(o instanceof NarseseTemplate))
                    return;

                if (r!=null) {
                    p.remove(r);                    
                }

                r = newPanel((NarseseTemplate)o, formSelect.getSelectedItem().toString());
                
                p.add(r, BorderLayout.CENTER);           
                p.validate();
            }
        };
        
        formSelect.addActionListener(change);
        t.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                change.actionPerformed(null);
            }
            
        });
        
        change.actionPerformed(null);
        
        
        return p;
    }
 
    
    
    public static void main(String[] args) {
        List<NarseseTemplate> templates = new ArrayList();
        templates.addAll(Arrays.asList( new NarseseTemplate[] {
            new NarseseTemplate("<~#a--> ~#b>? %~t%",  "Is ~#a is a ~#b? ~t"),
            new NarseseTemplate("<~#a--> ~#b>. %~t%",  "~#a is a ~#b. ~t"),
            new NarseseTemplate("<~#a --> ~#b>. %1.00;0.99%",  "~#a is a ~#b."),
            new NarseseTemplate("<~#a --> ~#b>. %0.00;0.99%",  "~#a is not a ~#b."),
            new NarseseTemplate("<~#a --> ~#b>. %1.00;0.50%",  "~#a is possibly a ~#b."),
            new NarseseTemplate("<~#a --> ~#b>. %0.00;0.50%",  "~#a is possibly not a ~#b."),            
        }));
        
        NWindow w = new NWindow("NarseseTemplatePanel test", NarseseTemplatePanel.newPanel(templates) );
        w.setSize(400, 200);
        w.setVisible(true);
    }
}
