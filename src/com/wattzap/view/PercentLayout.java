/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wattzap.view;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Constraints: left/rigth+top/bottom: percent size left might be west/east,
 * second value is offset from west/east border (exact size is taken from
 * preffered, but must be min..max) top might be north/south, same rules apply.
 * Always 4 values must be provided.
 *
 * @author Jarek
 */
public class PercentLayout implements LayoutManager2, Serializable {

    private class PercentConstraints {

        private final Component comp;
        private final Component parent;

        private float left = 0.0f;
        private boolean west = false;
        private float right = 1.0f;
        private boolean east = false;
        private float top = 0.0f;
        private boolean north = false;
        private float bottom = 1.0f;
        private boolean south = false;

        public PercentConstraints(Component comp, Object o) {
            this.comp = comp;
            this.parent = comp.getParent();

            if (o == null) {
                // nothing..
            } else if (o instanceof String) {
                String[] val = ((String) o).split(" *[-+/x] *");
                if (val.length != 4) {
                    throw new AWTError("Wrong number of fields (" + val.length
                            + ") in constraint '" + ((String) o) + "'");
                }

                if ("east".equals(val[0])) {
                    east = true;
                    left = 0.0f;
                    right = (float) (1.0 - Double.parseDouble(val[1]) / 100.0);
                } else if ("west".equals(val[0])) {
                    west = true;
                    left = (float) (Double.parseDouble(val[1]) / 100.0);
                    right = 1.0f;
                } else {
                    left = (float) (Double.parseDouble(val[0]) / 100.0);
                    right = (float) (Double.parseDouble(val[1]) / 100.0);
                }

                if ("south".equals(val[2])) {
                    south = true;
                    top = 0.0f;
                    bottom = (float) (1.0 - Double.parseDouble(val[3]) / 100.0);
                } else if ("north".equals(val[2])) {
                    north = true;
                    top = (float) (Double.parseDouble(val[3]) / 100.0);
                    bottom = 1.0f;
                } else {
                    top = (float) (Double.parseDouble(val[2]) / 100.0);
                    bottom = (float) (Double.parseDouble(val[3]) / 100.0);
                }

                if ((left >= right) || (left < 0.0f) || (right > 1.0f)) {
                    throw new AWTError("Wrong x location (" + left + ".." + right + ")");
                }
                if ((top >= bottom) || (top < 0.0f) || (bottom > 1.0f)) {
                    throw new AWTError("Wrong y location (" + top + ".." + bottom + ")");
                }
            } else {
                throw new AWTError("Constraint of " + o.getClass().getSimpleName()
                        + " not handled");
            }
        }

        public Component getComp() {
            return comp;
        }
        public Component getParent() {
            return parent;
        }

        // get bounds for component when main form is width x height
        // take care of min/max/preferred sizes
        public Rectangle getBounds(int width, int height) {
            Rectangle r = new Rectangle(0, 0, width, height);
            Dimension d;

            // get preferred size: useful when attached to the border
            d = comp.getPreferredSize();
            r.width = d.width;
            r.height = d.height;
            // if bigger than maximum
            d = comp.getMaximumSize();
            if (r.width > d.width) {
                r.width = d.width;
            }
            if (r.height > d.height) {
                r.height = d.height;
            }
            // if smaller than minimum
            d = comp.getMinimumSize();
            if (r.width < d.width) {
                r.width = d.width;
            }
            if (r.height < d.height) {
                r.height = d.height;
            }

            if (west) {
                r.x = (int) (left * width);
            } else if (east) {
                r.x = (int) (right * width) - r.width;
            } else {
                r.x = (int) (left * width);
                r.width = (int) ((right - left) * width);
            }
            // doesn't fit.. size must be overriden
            if (r.x < 0) {
                r.width -= r.x;
                r.x = 0;
            }
            if (r.x + r.width > width) {
                r.width = width - r.x;
            }

            if (north) {
                r.y = (int) (top * height);
            } else if (south) {
                r.y = (int) (bottom * height) - r.height;
            } else {
                r.y = (int) (top * height);
                r.height = (int) ((bottom - top) * height);
            }
            if (r.y < 0) {
                r.height -= r.y;
                r.y = 0;
            }
            if (r.y + r.height > height) {
                r.height = height - r.y;
            }
            return r;
        }

        // get size for which component meets constraint
        public Dimension getLayoutSize(Dimension d) {
            Dimension r = new Dimension();
            r.width = (int) (d.width / (right - left));
            r.height = (int) (d.height / (bottom - top));
            return r;
        }
    }

    private static final Map<Component, PercentConstraints> components = new HashMap<>();

    // minimal size to fit all components
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Dimension r = new Dimension(0, 0);
        for (PercentConstraints per : components.values()) {
            Dimension d = per.getLayoutSize(per.getComp().getMinimumSize());
            if (r.width < d.width) {
                r.width = d.width;
            }
            if (r.height < d.height) {
                r.height = d.height;
            }
        }
        return r;
    }

    // Biggest prefered size
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Dimension r = new Dimension(0, 0);
        for (PercentConstraints per : components.values()) {
            Dimension d = per.getLayoutSize(per.getComp().getPreferredSize());
            if (r.width < d.width) {
                r.width = d.width;
            }
            if (r.height < d.height) {
                r.height = d.height;
            }
        }
        return r;
    }

    // maximal size to fit all components
    @Override
    public Dimension maximumLayoutSize(Container target) {
        Dimension r = new Dimension(10000, 10000);
        for (PercentConstraints per : components.values()) {
            Dimension d = per.getLayoutSize(per.getComp().getMaximumSize());
            if (r.width > d.width) {
                r.width = d.width;
            }
            if (r.height > d.height) {
                r.height = d.height;
            }
        }
        return r;
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.0f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.0f;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        addLayoutComponent(comp, null);
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        assert !components.keySet().contains(comp) : "Component added twice:: " + comp;
        components.put(comp, new PercentConstraints(comp, constraints));
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        assert components.containsKey(comp) : "Component not added:: " + comp;
        components.remove(comp);
    }

    @Override
    public void invalidateLayout(Container target) {
    }

    @Override
    public void layoutContainer(Container parent) {
        Dimension target = parent.getSize();
        for (PercentConstraints c : components.values()) {
            if (c.getParent() == parent) {
                Rectangle r = c.getBounds(target.width, target.height);
                c.getComp().setBounds(r);
            }
        }
    }
}
