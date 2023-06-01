package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import org.jetbrains.annotations.Contract;

public class MyGdxGame extends ApplicationAdapter implements InputProcessor{

	//Objeto que recoge el mapa de baldosas
	private TiledMap mapa;

	//Capa del mapa donde se encuentran los tesoros
	private TiledMapTileLayer capaTesoros;

	//Ancho y alto del mapa en tiles
	private int anchoTiles, altoTiles;

	//Celda en la que el mapa finaliza
	private Vector2 celdaFinal;

	//Arrays bidimensionales de booleanos que contienen los obtaculos y los tesoros del mapa
	private boolean[][] obstaculo, tesoro;

	//Objeto con el que se pinta el mapa de baldosas
	private TiledMapRenderer mapaRenderer;

	//Variables de ancho y alto
	int anchoMapa, altoMapa, anchoCelda, altoCelda;

	//Variable para contabilizar el numero de tesoros
	int totalTesoros;

	// Cámara que nos da la vista del juego
	private OrthographicCamera camara;

	//Variables para las dimensiones de la pantalla
	private float anchuraPantalla, alturaPantalla;
	//Posición en el eje de coordenadas actual del jugador
	private Vector2 posicionJugador;

	//Velocidad de desplazamiento del jugador para cada iteración del bucle de renderizado
	private float velocidadJugador;


	//PNJ
	// Este atributo indica el tiempo en segundos transcurridos desde que se inicia la animación,
// servirá para determinar qué frame se debe representar
	private float stateTime;

	//Booleanos que determinan la dirección de marcha del sprite
	private static boolean izquierda, derecha, arriba, abajo;

	//Dimensiones del sprite
	private int anchoJugador;
	private int altoJugador;

	//Objeto que permite dibujar en el método render() imágenes 2D
	private SpriteBatch sb;

	//Constantes que indican el numero de filas y columnas de la hoja de sprites
	private static final int FRAME_COLS = 3;
	private static final int FRAME_ROWS = 4;

	// Atributo en el que se cargará la imagen del personaje principal.
	private Texture imagenPrincipal;

	//Animacion que se muestra en el metodo render()
	private Animation<TextureRegion> jugador;

	//Animaciones para cada una de las direcciones de mvto. del jugador
	private Animation<TextureRegion> jugadorArriba;
	private Animation<TextureRegion> jugadorDerecha;
	private Animation<TextureRegion> jugadorAbajo;
	private Animation<TextureRegion> jugadorIzquierda;

	public void create() {
		//cargamos el mapa de baldosas desde la carpeta de asets
		mapa = new TmxMapLoader().load("map/mapa.tmx");
		mapaRenderer = new OrthogonalTiledMapRenderer(mapa);

		//Determinamos el alto y ancho del mapa de baldosas. Para ello necesitamops extraer la
		//base del mapa y, a partir de ella, determinamos el número de celdas a lo ancho y alto
		//asi como el tamaño de la celda, que multiplicando por el numero de celdas a lo alto
		//ancho, da como resultado el alto y ancho en pixeles del mapa
		TiledMapTileLayer capa = (TiledMapTileLayer) mapa.getLayers().get(0);

		//Determinamos el ancho y alto de cada celda
		anchoCelda = capa.getTileWidth();
		altoCelda = capa.getTileHeight();

		//Determinamos el ancho y alto del mapa completo
		anchoMapa = capa.getWidth();
		altoMapa = capa.getHeight();

		//Cargamos las capas de los obstaculos y las de los pasos en el TiledMap
		TiledMapTileLayer capaSuelo = (TiledMapTileLayer) mapa.getLayers().get(0);
		TiledMapTileLayer capaSombras = (TiledMapTileLayer) mapa.getLayers().get(1);
		TiledMapTileLayer capaMuro1 = (TiledMapTileLayer) mapa.getLayers().get(2);
		TiledMapTileLayer capaPaso = (TiledMapTileLayer) mapa.getLayers().get(3);
		TiledMapTileLayer capaMuro2 = (TiledMapTileLayer) mapa.getLayers().get(4);
		capaTesoros = (TiledMapTileLayer) mapa.getLayers().get(5);
		TiledMapTileLayer capaArboles = (TiledMapTileLayer) mapa.getLayers().get(6);
		TiledMapTileLayer capaDetalles = (TiledMapTileLayer) mapa.getLayers().get(7);

		//el numero de tiles es igual en todas las capas. Lo tomamos de la capa Suelo
		anchoTiles = capaSuelo.getWidth();
		anchoTiles = capaSuelo.getHeight();

		//Creamos un array bidimensional de booleanos para obstaculos y tesoros
		obstaculo = new boolean[anchoTiles][altoTiles];
		tesoro = new boolean[anchoTiles][altoTiles];

		//Rellenamos los valores recorriendo el mapa
		for (int i = 0; i < anchoTiles; i++) {
			for (int j = 0; j < altoTiles; j++) {
				//rellenamos el array bidimensional de los obstaculos
				obstaculo[i][j] = ((capaMuro1.getCell(i, j) != null)//obstaculos de la capa muro
						&& (capaPaso.getCell(i, j) == null)); // que no sean pasos permitidos

				//rellenamos edl array bidimensional de los tesoros
				tesoro[i][j] = (capaTesoros.getCell(i, j) != null);
				if (tesoro[i][j]) {
					totalTesoros++;
				}
			}
		}

		//Posiociones inicial y final del recorrido
		Vector2 celdaInicial = new Vector2(1, 1);
		celdaFinal = new Vector2(16, 2);

		//Inicializamos la cámara del juego
		anchuraPantalla = Gdx.graphics.getWidth();
		alturaPantalla = Gdx.graphics.getHeight();

		posicionJugador = new Vector2(posicionaMapa(celdaInicial));
		//Creamos una cámara que mostrará una zona del mapa (igual en todas las plataformas)
		int anchoCamara = 400, altoCamara = 240;
		camara = new OrthographicCamera(anchoCamara, altoCamara);

		//Actualizamos la posición de la cámara
		camara.update();

		//Ponemos a cero el atributo stateTime, que marca el tiempo de ejecución de la animación del personaje principal
		stateTime = 0f;
		//Cargamos la imagen del personaje principal en el objeto img de la clase Texture
		imagenPrincipal = new Texture(Gdx.files.internal("pnj/andar.png"));

		//Sacamos los frames de img en un array bidimensional de TextureRegion
		TextureRegion[][] tmp = TextureRegion.split(imagenPrincipal, imagenPrincipal.getWidth() / FRAME_COLS, imagenPrincipal.getHeight() / FRAME_ROWS);

		//Creamos el objeto SpriteBatch que nos permitirá crear animaciones dentro del método render()
		sb = new SpriteBatch();
		//Tile Inicial y Final
		celdaInicial = new Vector2(0, 0);
		celdaFinal = new Vector2(24, 1); //el tile final en el mapa de ejemplo

		//Creamos las distintas animaciones en bucle, teniendo en cuenta que el timepo entre frames será 150 milisegundos

		float frameJugador = 0.15f;

		jugadorAbajo = new Animation<>(frameJugador, tmp[0]); //Fila 0, dirección abajo
		jugadorAbajo.setPlayMode(Animation.PlayMode.LOOP);
		jugadorIzquierda = new Animation<>(frameJugador, tmp[1]); //Fila 1, dirección izquierda
		jugadorIzquierda.setPlayMode(Animation.PlayMode.LOOP);
		jugadorDerecha = new Animation<>(frameJugador, tmp[2]); //Fila 2, dirección derecha
		jugadorDerecha.setPlayMode(Animation.PlayMode.LOOP);
		jugadorArriba = new Animation<>(frameJugador, tmp[3]); //Fila 3, dirección arriba
		jugadorArriba.setPlayMode(Animation.PlayMode.LOOP);

		//En principio se utiliza la animación en la dirección abajo
		jugador = jugadorAbajo;

		//Dimensiones del jugador
		anchoJugador = tmp[0][0].getRegionWidth();
		altoJugador = tmp[0][0].getRegionHeight();
		//Variable para contar los tesoros recogidos
		//cuentaTesoros = 0;

		//Velocidad del jugador (puede hacerse un menú de configuración para cambiar la dificultad del juego)
		velocidadJugador = 0.75f;
	}

	public void render() {

		//ponemos a la escucha de eventos la propia clase del juego
		Gdx.input.setInputProcessor(this);

		//Centramos la camara en el jugador principal
		camara.position.set(posicionJugador, 0);

		//Comprobamos que la cámara no se salga de los límites del mapa de baldosas con el método MathUtils.clamp
		camara.position.x = MathUtils.clamp(camara.position.x,
				camara.viewportWidth / 2f,
				anchoMapa - camara.viewportWidth / 2f);
		camara.position.y = MathUtils.clamp(camara.position.y,
				camara.viewportHeight / 2f,
				altoMapa - camara.viewportHeight / 2f);

		//Actualizamos la cámara del juego
		camara.update();
		//Vinculamos el objeto que dibuja el mapa con la cámara del juego
		mapaRenderer.setView(camara);

		//Para borrar la pantalla
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		//Vinculamos el objeto que dibuja el mapa con la cámara del juego
		mapaRenderer.setView(camara);

		//Dibujamos las capas del mapa
		//Posteriormente quitaremos la capa de profundidad para intercalar a los personajes
		int[] capas = {0, 1, 2, 3, 4,5,6,7};
		mapaRenderer.render(capas);

		//ANIMACION DEL JUGADOR

		//En este método actualizaremos la posición del jugador principal
		actualizaPosicionJugador();

		// Indicamos al SpriteBatch que se muestre en el sistema de coordenadas específicas de la cámara.
		sb.setProjectionMatrix(camara.combined);

		//Inicializamos el objeto SpriteBatch
		sb.begin();

		//cuadroActual contendrá el frame que se va a mostrar en cada momento.
		TextureRegion cuadroActual = jugador.getKeyFrame(stateTime);
		sb.draw(cuadroActual, posicionJugador.x, posicionJugador.y);

		//Finalizamos el objeto SpriteBatch
		sb.end();
	}



	public void dispose() {
		//Texture
		imagenPrincipal.dispose();
		//SpriteBatch
		if (sb.isDrawing())
			sb.dispose();
	}

	public Vector2 posicionaMapa(Vector2 celda) {
		Vector2 res = new Vector2();
		if (celda.x + 1 > anchoTiles ||
				celda.y + 1 > altoTiles) {  //Si la peticion esta mal, situamos en el origen del mapa
			res.set(0, 0);
		}
		res.x = celda.x * anchoCelda;
		res.y = (altoTiles - 1 - celda.y) * altoCelda;
		return res;
	}

	private void actualizaPosicionJugador() {

		//Guardamos la posicion del jugador por si encontramos algun obstaculo
		Vector2 posicionAnterior = new Vector2();
		posicionAnterior.set(posicionJugador);

		//Los booleanos izquierda, derecha, arriba y abajo recogen la dirección del personaje,
		//para permitir direcciones oblícuas no deben ser excluyentes.
		//Pero sí debemos excluir la simultaneidad entre arriba/abajo e izquierda/derecha
		//para no tener direcciones contradictorias
		if (izquierda) {
			posicionJugador.x -= velocidadJugador;
			jugador = jugadorIzquierda;
		}
		if (derecha) {
			posicionJugador.x += velocidadJugador;
			jugador = jugadorDerecha;
		}
		if (arriba) {
			posicionJugador.y += velocidadJugador;
			jugador = jugadorArriba;
		}
		if (abajo) {
			posicionJugador.y -= velocidadJugador;
			jugador = jugadorAbajo;
		}

		//Avanzamos el stateTime del jugador principal cuando hay algún estado de movimiento activo
		if (izquierda || derecha || arriba || abajo) {
			stateTime += Gdx.graphics.getDeltaTime();
		}

		//Limites en el mapa para el jugador
		posicionJugador.x = MathUtils.clamp(posicionJugador.x, 0, anchoMapa - anchoJugador);
		posicionJugador.y = MathUtils.clamp(posicionJugador.y, 0, altoMapa - altoJugador);

		//Detección de obstaculos
		if (obstaculo(posicionJugador))
			posicionJugador.set(posicionAnterior);

		//Deteccion de fin del mapa
		if (celdaActual(posicionJugador).epsilonEquals(celdaFinal)) {
			//Paralizamos el juego 1 segundo para reproducir algún efecto sonoro
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//Código del final del juego
		}
	}

	private boolean obstaculo(Vector2 posicion) {
		int limIzq = (int) ((posicion.x + 0.25 * anchoJugador) / anchoCelda);
		int limDrcha = (int) ((posicion.x + 0.75 * anchoJugador) / anchoCelda);
		int limSup = (int) ((posicion.y + 0.25 * altoJugador) / altoCelda);
		int limInf = (int) ((posicion.y) / altoCelda);

		return obstaculo[limIzq][limInf] || obstaculo[limDrcha][limSup] || obstaculo[limDrcha][limInf] || obstaculo[limIzq][limSup];
	}


	@Override
	public boolean keyDown(int keycode) {
		switch (keycode) {
			case Input.Keys.LEFT:
				setIzquierda(true);
				break;
			case Input.Keys.RIGHT:
				setDerecha(true);
				break;
			case Input.Keys.UP:
				setArriba(true);
				break;
			case Input.Keys.DOWN:
				setAbajo(true);
				break;
		}
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		switch (keycode) {
			case Input.Keys.LEFT:
				setIzquierda(false);
				break;
			case Input.Keys.RIGHT:
				setDerecha(false);
				break;
			case Input.Keys.UP:
				setArriba(false);
				break;
			case Input.Keys.DOWN:
				setAbajo(false);
				break;
		}
			//Para ocultar/mostrar las distintas capas pulsamos desde el 1 en adelante...
			int codigoCapa = keycode - Input.Keys.NUM_1;
			if (codigoCapa <= 4)
				mapa.getLayers().get(codigoCapa).setVisible(!mapa.getLayers().get(codigoCapa).isVisible());

			return true;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector3 clickCoordinates = new Vector3(screenX, screenY, 0f);
			//Transformamos las coordenadas del vector a coordenadas de nuestra camara
			Vector3 pulsacion3d = camara.unproject(clickCoordinates);
			Vector2 pulsacion = new Vector2(pulsacion3d.x, pulsacion3d.y);

			//Calculamos la diferencia entre la pulsacion y el centro del jugador
			Vector2 centroJugador = new Vector2(posicionJugador).add((float) anchoJugador / 2, (float) altoJugador / 2);
			Vector2 diferencia = new Vector2(pulsacion.sub(centroJugador));

			//Vamos a determinar la intencion del usuario para mover al personaje en funcion del
			//angulo entre la pulsacion y la posicion del jugador
			float angulo = diferencia.angleDeg();

			if (angulo > 30 && angulo <= 150) setArriba(true);
			if (angulo > 120 && angulo <= 240) setIzquierda(true);
			if (angulo > 210 && angulo <= 330) setAbajo(true);
			if ((angulo > 0 && angulo <= 60) || (angulo > 300 && angulo < 360)) setDerecha(true);

			return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			setArriba(false);
			setAbajo(false);
			setIzquierda(false);
			setDerecha(false);

			return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		//mismo caso que touchDown
			touchDown(screenX,screenY,pointer,0);
			return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}
		//Método que convierte la posición del jugador en la celda en la que está
		private Vector2 celdaActual(Vector2 posicion) {
			return new Vector2((int) (posicion.x / anchoCelda), (altoTiles - 1 - (int) (posicion.y / altoCelda)));
		}




		//Con estos setters se impide la situacion de direcciones contradictorias pero no las
		//direcciones compuestas que permiten movimientos oblícuos

		private void setIzquierda(boolean izq) {
			if (derecha && izq) derecha = false;
			izquierda = izq;
		}

		private void setDerecha(boolean der) {
			if (izquierda && der) izquierda = false;
			derecha = der;
		}

		private void setArriba(boolean arr) {
			if (abajo && arr) abajo = false;
			arriba = arr;
		}

		private void setAbajo(boolean abj) {
			if (arriba && abj) arriba = false;
			abajo = abj;
		}

}


