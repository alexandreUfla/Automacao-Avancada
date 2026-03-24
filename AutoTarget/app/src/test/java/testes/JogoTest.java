package testes;

import org.junit.Test;
import static org.junit.Assert.*;

import classes.Alvo;
import classes.AlvoComum;
import classes.Jogo;
import classes.JogoException;
import classes.Projetil;

public class JogoTest {
    @Test
    public void testPosicaoCanhaoInvalida(){
        Jogo jogo = new Jogo();
        // Espera-se que lance a exceção  JogoException por estar fora da tela (-50, -50)
        Exception exception = assertThrows(JogoException.class, () -> {
            jogo.adicionarCanhao(-50,-50);
        });
        assertTrue(exception.getMessage().contains("fora dos limites"));
    }

    @Test
    public void testLimiteMaximoCanhoes() throws JogoException{
        Jogo jogo = new Jogo();
        // Adiciona 10 canhões na esquerda
        for (int i = 0; i < 10; i++){
            jogo.adicionarCanhao(100,100);
        }
        // O 11º canhão no mesmo lado deve lançar erro
        Exception exception = assertThrows(JogoException.class, () -> {
           jogo.adicionarCanhao(100,100);
        });
        assertTrue(exception.getMessage().contains("Limite máximo"));
    }

    @Test
    public void testeVerificacaoColisao(){
        Jogo jogo = new Jogo();
        // Cria um alvo na posição (100,100)
        Alvo alvo = new AlvoComum(100,100,jogo);
        jogo.adicionarAlvo(alvo);

        // Cria projétil na exata mesma posição
        Projetil p = new Projetil(100,100,100,100,jogo,true);

        // Verifica colisão
        jogo.verificarColisao(p);

        // O alvo deve ter sido removido e marcado como inativo
        assertFalse(alvo.isAtivo());
    }
}
