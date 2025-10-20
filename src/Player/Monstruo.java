package Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Monstruo {
    public ArrayList<Enemigo> enemigos = new ArrayList<>();
    private int anchoPantalla = 0, altoPantalla = 0, margen = 0;

    public static int ENEMY_SIZE = 56;      // ↓ más pequeño
    public static final int SPEED_BASE = 3; // 🔁 misma velocidad base para TODOS (jugador y enemigos)

    private final GameSettings settings;
    private double extraSpeedMul = 1.0;

    public Monstruo(GameSettings settings) {
        this.settings = settings;
    }

    public void setExtraSpeedMul(double mul) { this.extraSpeedMul = Math.max(0.5, mul); }
    public void setLimitesPantalla(int ancho, int alto, int margen) {
        this.anchoPantalla=ancho; this.altoPantalla=alto; this.margen=Math.max(0,margen);
    }

    // Spawns distribuidos: esquinas + centros de bordes
    private List<Point> spawnPoints() {
        int w = Math.max(400, anchoPantalla), h = Math.max(300, altoPantalla), m = Math.max(40, margen);
        int cx = w/2, cy = h/2;
        return List.of(
                new Point(m + 40, m + 40),                   // esquina sup-izq
                new Point(w - m - 120, m + 40),              // esquina sup-der
                new Point(m + 40, h - m - 120),              // esquina inf-izq
                new Point(w - m - 120, h - m - 120),         // esquina inf-der
                new Point(cx - 60, m + 40),                  // borde superior centro
                new Point(cx - 60, h - m - 120),             // borde inferior centro
                new Point(m + 40, cy - 60),                  // borde izq centro
                new Point(w - m - 120, cy - 60)              // borde der centro
        );
    }

    public void spawnRonda(int multiplicador) {
        enemigos.clear();
        List<Point> spawns = spawnPoints();
        int s = spawns.size(); if (s == 0) spawns = List.of(new Point(100,100));

        // repartimos por puntos con round-robin para dar tiempo de reacción
        addMany(multiplicador, "/Imagenes/Mush.gif",      50, SPEED_BASE,10,false, spawns);
        addMany(multiplicador, "/Imagenes/Slime.gif",     60, SPEED_BASE,10,false, spawns);
        addMany(multiplicador, "/Imagenes/Bat.gif",       40, SPEED_BASE, 8,true,  spawns);
        addMany(multiplicador, "/Imagenes/Icebat.gif",    50, SPEED_BASE, 8,true,  spawns);
        addMany(multiplicador, "/Imagenes/Evil Cube.gif", 80, SPEED_BASE,12,false, spawns);
    }

    private void addMany(int count, String ruta, int vida, int vel, int dmg, boolean puedeDisparar, List<Point> spawns) {
        int s = spawns.size();
        for (int i = 0; i < count; i++) {
            int vidaS = (int)Math.round(vida * settings.enemyHealthMul);
            int velS  = Math.max(1, (int)Math.round(vel * settings.enemySpeedMul * extraSpeedMul));
            int dmgS  = Math.max(1, (int)Math.round(dmg * settings.enemyDamageMul));
            Point p = spawns.get(i % s);
            int jitterX = (i%3)*14, jitterY = (i%2)*12; // pequeño desplazamiento para no solapar exacto
            enemigos.add(new Enemigo(p.x + jitterX, p.y + jitterY, ruta, vidaS, velS, dmgS, puedeDisparar));
        }
    }

    public void update(Personajes p) {
        for (Enemigo e : enemigos) e.update(p);

        for (int i=0;i<enemigos.size();i++){
            Enemigo e1 = enemigos.get(i);
            for (int j=i+1;j<enemigos.size();j++){
                Enemigo e2 = enemigos.get(j);
                if (e1.getBounds().intersects(e2.getBounds())){
                    int dx=e2.x-e1.x, dy=e2.y-e1.y; if (dx==0&&dy==0) dx=1;
                    double dist=Math.max(1,Math.hypot(dx,dy));
                    e1.x -= (int)(dx/dist*2); e1.y -= (int)(dy/dist*2);
                    e2.x += (int)(dx/dist*2); e2.y += (int)(dy/dist*2);
                }
            }
            if (anchoPantalla>0 && altoPantalla>0){
                int minX=margen, minY=margen;
                int maxX=Math.max(minX, anchoPantalla-margen-ENEMY_SIZE);
                int maxY=Math.max(minY, altoPantalla-margen-ENEMY_SIZE);
                e1.x = Math.max(minX, Math.min(e1.x, maxX));
                e1.y = Math.max(minY, Math.min(e1.y, maxY));
            }
            if (!e1.disparos.isEmpty() && anchoPantalla>0 && altoPantalla>0){
                for (int k=0;k<e1.disparos.size();k++){
                    Proyectil d=e1.disparos.get(k);
                    Rectangle r=d.getBounds();
                    if (r.x+r.width<margen||r.x>anchoPantalla-margen||r.y+r.height<margen||r.y>altoPantalla-margen||!d.activo){
                        e1.disparos.remove(k--);
                    }
                }
            }
        }
    }

    public void draw(Graphics g) { for (Enemigo e: enemigos) e.draw(g); }

    public static class Enemigo {
        public int x,y,velocidad,vida,vidaMax,daño;
        public boolean puedeDisparar;
        public ImageIcon sprite;
        public ArrayList<Proyectil> disparos = new ArrayList<>();
        private int disparoCooldown=0;

        public Enemigo(int x,int y,String ruta,int vida,int vel,int dmg,boolean puedeDisparar){
            this.x=x; this.y=y;
            this.vida=vida; this.vidaMax=Math.max(1,vida);
            this.velocidad=vel; this.daño=dmg;
            this.puedeDisparar=puedeDisparar;
            this.sprite=new ImageIcon(getClass().getResource(ruta));
        }

        public void update(Personajes p){
            double dx=p.x-x, dy=p.y-y;
            double dist=Math.max(1,Math.hypot(dx,dy));
            x += (int)Math.round(velocidad*dx/dist);
            y += (int)Math.round(velocidad*dy/dist);

            if (disparoCooldown>0) disparoCooldown--;
            else if (puedeDisparar){
                int vx=(int)Math.signum(dx), vy=(int)Math.signum(dy);
                if (vx==0 && vy==0) vy=1;
                Proyectil pj = new Proyectil(x+ENEMY_SIZE/2, y+ENEMY_SIZE/2, vx, vy);
                pj.daño = daño;
                pj.conTamaño(8);
                disparos.add(pj);
                disparoCooldown = 60;
            }

            for (int i=0;i<disparos.size();i++){
                Proyectil d=disparos.get(i);
                d.update();
                if (!d.activo){ disparos.remove(i); i--; }
            }
        }

        public void draw(Graphics g){
            g.drawImage(sprite.getImage(), x, y, ENEMY_SIZE, ENEMY_SIZE, null);
            int barW=40, barH=6, ox=(ENEMY_SIZE-barW)/2;
            g.setColor(Color.RED);   g.fillRect(x+ox, y-8, barW, barH);
            g.setColor(Color.GREEN); g.fillRect(x+ox, y-8, (int)Math.round(barW*(vida/(double)vidaMax)), barH);
            for (Proyectil d: disparos) d.draw(g);
        }

        public void recibirDaño(int dmg){ vida = Math.max(0, vida - dmg); }
        public Rectangle getBounds(){ return new Rectangle(x, y, ENEMY_SIZE, ENEMY_SIZE); }
    }
}
