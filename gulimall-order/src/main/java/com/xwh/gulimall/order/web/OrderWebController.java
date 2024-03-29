package com.xwh.gulimall.order.web;


import com.xwh.common.exception.NoStockException;
import com.xwh.gulimall.order.service.OrderService;
import com.xwh.gulimall.order.vo.OrderConfirmVo;
import com.xwh.gulimall.order.vo.OrderSubmitVo;
import com.xwh.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) {
        OrderConfirmVo confirmVo = null;
        try {
            confirmVo = orderService.confirmOrder();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     *
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {
        String msg = "";
        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            // 下单成功来支付选择页
            // 下单失败回到订单确认页重新确认订单信息
            if (responseVo.getCode() == 0) {
                model.addAttribute("submitOrderResp", responseVo);
                return "pay";
            } else {
                switch (responseVo.getCode()) {
                    case 1:
                        msg += "订单信息过期，请刷新再次提交";
                        break;
                    case 2:
                        msg += "订单商品价格发生变化，请确认后再次提交";
                        break;
                    case 3:
                        msg += "库存锁定失败，商品库存不足";
                        break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (Exception e) {
            if (e instanceof NoStockException) {
                msg = e.getMessage();
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
