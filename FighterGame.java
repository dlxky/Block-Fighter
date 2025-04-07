import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * A 2D fighting game featuring two players with melee attacks, projectiles, and shield mechanics.
 */
public class FighterGame extends JFrame {

    /**
     * Constructs the main game window.
     */
    public FighterGame() {
        add(new GamePanel());
        setTitle("BLOCK Fighter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Main entry point for the application.
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        new FighterGame();
    }
}

/**
 * The main game panel handling all game logic and rendering.
 */
class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;
    private static final int GROUND_Y = 340;
    
    private final Player p1;
    private final Player p2;
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final javax.swing.Timer gameTimer;
    private final ArrayList<SlashEffect> slashEffects = new ArrayList<>();
    private final Set<Integer> pressedKeys = new HashSet<>();

    /**
     * Initializes the game panel with players and game state.
     */
    public GamePanel() {
        p1 = new Player(50, GROUND_Y - 40, Color.RED, KeyEvent.VK_A, KeyEvent.VK_D, 
                       KeyEvent.VK_W, KeyEvent.VK_Q, KeyEvent.VK_E);
        p2 = new Player(700, GROUND_Y - 40, Color.BLUE, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                       KeyEvent.VK_UP, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2);
        
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        addKeyListener(this);
        
        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();
    }

    /**
     * Represents a player character with combat capabilities.
     */
    private class Player {
        /** Character's X position */
        int x;
        /** Character's Y position */
        int y;
        /** Current health points */
        int health = 100;
        /** Current shield energy */
        int shield = 100;
        /** Shield activation state */
        boolean shielding = false;
        /** Character color */
        final Color color;
        /** Key bindings */
        final int leftKey, rightKey, jumpKey, attackKey, projectileKey;
        /** Grounded state */
        boolean onGround = false;
        /** Vertical velocity */
        double yVelocity = 0;
        /** Facing direction (1 = right, -1 = left) */
        int facingDirection = 1;

        /**
         * Constructs a new player character.
         * @param x Initial X position
         * @param y Initial Y position
         * @param color Character color
         * @param left Left movement key
         * @param right Right movement key
         * @param jump Jump key
         * @param attack Melee attack key
         * @param projectile Projectile attack key
         */
        public Player(int x, int y, Color color, int left, int right, int jump, int attack, int projectile) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.leftKey = left;
            this.rightKey = right;
            this.jumpKey = jump;
            this.attackKey = attack;
            this.projectileKey = projectile;
        }
    }

    /**
     * Represents a projectile attack.
     */
    private class Projectile {
        /** Current X position */
        int x;
        /** Current Y position */
        int y;
        /** Horizontal velocity */
        final int dx;
        /** Projectile color */
        final Color color;
        /** Owner of the projectile */
        final Player owner;
        
        /**
         * Constructs a new projectile.
         * @param x Initial X position
         * @param y Initial Y position
         * @param dx Horizontal velocity
         * @param color Projectile color
         * @param owner Owning player
         */
        public Projectile(int x, int y, int dx, Color color, Player owner) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.color = color;
            this.owner = owner;
        }
    }

    /**
     * Represents a melee attack visual effect.
     */
    private class SlashEffect {
        /** X position */
        int x;
        /** Y position */
        int y;
        /** Direction of the slash */
        int direction;
        /** Transparency level */
        float alpha = 1.0f;
        
        /**
         * Constructs a new slash effect.
         * @param x X position
         * @param y Y position
         * @param direction Slash direction (1 = right, -1 = left)
         */
        public SlashEffect(int x, int y, int direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
        }
    }

    /**
     * Updates player physics and facing direction.
     * @param p Player to update
     */
    private void updatePlayer(Player p) {
        // Update facing direction based on opponent position
        Player opponent = (p == p1) ? p2 : p1;
        p.facingDirection = (p.x < opponent.x) ? 1 : -1;

        // Apply gravity
        if (!p.onGround) {
            p.yVelocity += 0.5;
            p.y += p.yVelocity;
        }
        
        // Ground collision
        if (p.y >= GROUND_Y - 40) {
            p.y = GROUND_Y - 40;
            p.yVelocity = 0;
            p.onGround = true;
        }
    }

    /**
     * Checks and resolves all collisions in the game.
     */
    private void checkCollisions() {
        // Projectile collisions
        for (Iterator<Projectile> it = projectiles.iterator(); it.hasNext();) {
            Projectile proj = it.next();
            proj.x += proj.dx;
            
            for (Player p : new Player[]{p1, p2}) {
                if (p == proj.owner) continue;
                
                if (Math.abs(proj.x - p.x) < 30 && Math.abs(proj.y - p.y) < 40) {
                    if (p.shielding) {
                        p.shield = Math.max(0, p.shield - 15);
                    } else {
                        p.health = Math.max(0, p.health - 10);
                    }
                    it.remove();
                    break;
                }
            }
            
            if (proj.x < 0 || proj.x > WIDTH) it.remove();
        }
    }

    /**
     * Resets the game to its initial state.
     */
    private void newGame() {
        p1.x = 50;
        p1.y = GROUND_Y - 40;
        p1.health = 100;
        p1.shield = 100;
        
        p2.x = 700;
        p2.y = GROUND_Y - 40;
        p2.health = 100;
        p2.shield = 100;
        
        projectiles.clear();
        slashEffects.clear();
        pressedKeys.clear();
        gameTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw ground
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
        
        // Draw players
        drawPlayer(g2d, p1);
        drawPlayer(g2d, p2);
        
        // Draw shield bubbles
        drawShield(g2d, p1);
        drawShield(g2d, p2);
        
        // Draw projectiles
        for (Projectile proj : projectiles) {
            g2d.setColor(proj.color);
            g2d.fillOval(proj.x, proj.y, 10, 10);
        }
        
        // Draw slash effects
        for (SlashEffect slash : slashEffects) {
            drawSlash(g2d, slash);
        }
        
        // Draw UI
        drawStatusBars(g2d);
    }

    /**
     * Draws the shield effect around a player.
     * @param g2d Graphics context
     * @param p Player to draw shield for
     */
    private void drawShield(Graphics2D g2d, Player p) {
        if (p.shielding && p.shield > 0) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2d.setColor(new Color(135, 206, 235, 100));
            g2d.fillOval(p.x - 25, p.y - 20, 50, 50);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    /**
     * Draws a slash effect.
     * @param g2d Graphics context
     * @param slash Slash effect to draw
     */
    private void drawSlash(Graphics2D g2d, SlashEffect slash) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, slash.alpha));
        g2d.setColor(new Color(255, 255, 0, (int)(255 * slash.alpha)));
        
        int[] xPoints = {slash.x, slash.x + 20 * slash.direction, slash.x + 10 * slash.direction};
        int[] yPoints = {slash.y - 10, slash.y + 10, slash.y - 5};
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    /**
     * Draws a player character.
     * @param g2d Graphics context
     * @param p Player to draw
     */
    private void drawPlayer(Graphics2D g2d, Player p) {
        // Body
        g2d.setColor(p.color);
        g2d.fillRect(p.x - 15, p.y, 30, 40);
        // Head
        g2d.fillOval(p.x - 10, p.y - 10, 20, 20);
    }

    /**
     * Draws all status bars.
     * @param g2d Graphics context
     */
    private void drawStatusBars(Graphics2D g2d) {
        drawStatusBar(g2d, p1, 20, 20);
        drawStatusBar(g2d, p2, WIDTH - 220, 20);
    }

    /**
     * Draws an individual status bar.
     * @param g2d Graphics context
     * @param p Player to display status for
     * @param x X position
     * @param y Y position
     */
    private void drawStatusBar(Graphics2D g2d, Player p, int x, int y) {
        // Health bar
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, 200, 20);
        g2d.setColor(Color.RED);
        g2d.fillRect(x + 1, y + 1, (int)(1.98 * p.health), 18);
        
        // Shield bar
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y + 25, 200, 10);
        g2d.setColor(Color.CYAN);
        g2d.fillRect(x + 1, y + 26, (int)(1.98 * p.shield), 8);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    /**
     * Handles continuous player movement.
     * @param p Player to handle movement for
     */
    private void handleMovement(Player p) {
        if (pressedKeys.contains(p.leftKey)) p.x -= 5;
        if (pressedKeys.contains(p.rightKey)) p.x += 5;
        
        if (pressedKeys.contains(p.jumpKey) && p.onGround) {
            p.yVelocity = -12;
            p.onGround = false;
            pressedKeys.remove(p.jumpKey);
        }
        
        p.shielding = (p == p1 && pressedKeys.contains(KeyEvent.VK_SHIFT)) ||
                     (p == p2 && pressedKeys.contains(KeyEvent.VK_CONTROL));
    }

    /**
     * Handles player attacks.
     * @param p Player performing the attack
     * @param attackKey Melee attack key
     * @param projectileKey Projectile attack key
     */
    private void handleAttack(Player p, int attackKey, int projectileKey) {
        if (pressedKeys.contains(attackKey)) {
            slashEffects.add(new SlashEffect(
                p.x + (p.facingDirection == 1 ? 30 : -30),
                p.y + 20,
                p.facingDirection
            ));
            
            Player opponent = (p == p1) ? p2 : p1;
            if (Math.abs(p.x - opponent.x) < 50 && Math.abs(p.y - opponent.y) < 50) {
                if (opponent.shielding) {
                    opponent.shield = Math.max(0, opponent.shield - 7);
                } else {
                    opponent.health = Math.max(0, opponent.health - 5);
                }
            }
            pressedKeys.remove(attackKey);
        }
        
        if (pressedKeys.contains(projectileKey) && projectiles.size() < 3) {
            int dx = (p == p1) ? 5 : -5;
            projectiles.add(new Projectile(
                p.x + (dx > 0 ? 30 : -30),
                p.y + 20,
                dx,
                p.color,
                p
            ));
            pressedKeys.remove(projectileKey);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Handle continuous movements
        handleMovement(p1);
        handleMovement(p2);
        
        // Handle attacks
        handleAttack(p1, KeyEvent.VK_Q, KeyEvent.VK_E);
        handleAttack(p2, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2);
        
        // Update effects
        for (Iterator<SlashEffect> it = slashEffects.iterator(); it.hasNext();) {
            SlashEffect slash = it.next();
            slash.alpha -= 0.1f;
            if (slash.alpha <= 0) it.remove();
        }
        
        // Check game over
        if (p1.health <= 0 || p2.health <= 0) {
            gameTimer.stop();
            String winner = p1.health > 0 ? "Red Player" : "Blue Player";
            int choice = JOptionPane.showConfirmDialog(this, 
                winner + " Wins! Play again?", 
                "Game Over", 
                JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) newGame();
            else System.exit(0);
            return;
        }
        
        // Update game state
        updatePlayer(p1);
        updatePlayer(p2);
        checkCollisions();
        
        // Regenerate shields
        for (Player p : new Player[]{p1, p2}) {
            if (!p.shielding && p.shield < 100) p.shield += 0.5;
        }
        
        repaint();
    }

    @Override 
    public void keyTyped(KeyEvent e) {}
}