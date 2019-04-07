import com.lqh.Application;
import com.lqh.entity.Seckill;
import com.lqh.mapper.SeckillMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class SeckillMapperTest {
    @Autowired
    private SeckillMapper seckillMapper;

    @Test
    public void findAll() {
        List<Seckill> list = seckillMapper.findAll();
        for (Seckill seckill:list){
            System.out.println(seckill);
        }
    }

    @Test
    public void findById() {
        Seckill s = seckillMapper.findById(1);
        System.out.println(s);
    }

    @Test
    public void reduceStock() {
    }
}
