package com.murilloskills.utils;
import com.murilloskills.api.SkillContext;
import com.murilloskills.skills.*;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.network.Wire;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import java.util.*;

/** Two server-picked corners; the bounded preview is also the placement plan. */
public final class BrushTools {
    private BrushTools(){}
    public static void clear(SkillContext c){
        boolean marked=c.runtime().brushFirst!=null;c.runtime().brushFirst=null;c.runtime().brushSecond=null;c.runtime().brushDimension="";
        c.runtime().activities.remove("brush");if(marked)Wire.markers(c.player(),List.of(),"place",1);
    }
    public static boolean ready(SkillContext c){return c.active(SkillType.BUILDER)&&c.profile().equipped==SkillType.BUILDER&&c.runtime().brushFirst!=null&&c.runtime().brushSecond!=null;}
    public static String mark(SkillContext c){
        if(!c.active(SkillType.BUILDER)||c.profile().equipped!=SkillType.BUILDER)return "Ative o Pincel construtor primeiro.";
        var hit=c.player().pick(c.player().blockInteractionRange(),0,false);
        if(!(hit instanceof BlockHitResult b)||hit.getType()!=HitResult.Type.BLOCK)return "Aponte para uma superfície.";
        var pos=b.getBlockPos().relative(b.getDirection());if(!AreaTools.valid(c,pos))return "Ponto fora do alcance permitido.";
        var r=c.runtime();
        if(r.brushFirst==null||r.brushSecond!=null){r.brushFirst=pos.immutable();r.brushSecond=null;r.brushDimension=c.player().level().dimension().identifier().toString();Wire.notice(c,"brush","first","Pincel · ponto 1",pos.toShortString()+" · clique no segundo canto",0,200);return "Ponto 1: "+pos.toShortString()+". Marque o segundo canto.";}
        r.brushSecond=pos.immutable();
        if(volume(c)>BuilderSkill.BRUSH_AREA.value(c,SkillType.BUILDER)){r.brushSecond=null;return "Volume maior que o limite de "+(int)BuilderSkill.BRUSH_AREA.value(c,SkillType.BUILDER)+" blocos. Escolha um ponto mais próximo.";}
        preview(c);return "Pontos definidos. Confirme com UltPlace; materiais são consumidos.";
    }
    public static long volume(SkillContext c){var a=c.runtime().brushFirst;var b=c.runtime().brushSecond;return a==null||b==null?0:(Math.abs((long)a.getX()-b.getX())+1)*(Math.abs((long)a.getY()-b.getY())+1)*(Math.abs((long)a.getZ()-b.getZ())+1);}
    public static List<BlockPos> plan(SkillContext c){
        if(!ready(c)||volume(c)>BuilderSkill.BRUSH_AREA.value(c,SkillType.BUILDER))return List.of();
        var a=c.runtime().brushFirst;var b=c.runtime().brushSecond;
        List<BlockPos> out=new ArrayList<>();
        for(var p:BlockPos.betweenClosed(a,b))if(AreaTools.placeable(c,p))out.add(p.immutable());
        return List.copyOf(out);
    }
    public static void preview(SkillContext c){if(ready(c)){
        var plan=plan(c);Wire.markers(c.player(),plan,"place",30);
        c.runtime().activities.put("brush",new com.murilloskills.network.ActivityView("brush","ready","Pincel · confirmar",plan.size()+" blocos · "+c.player().getMainHandItem().getCount()+" materiais · confirme com UltPlace",2,2,plan.size(),c.now()+40));
    }}
    public record Result(com.murilloskills.network.Protocol.ResultCode code,String message,int affected){}
    public static String execute(SkillContext c){return executeResult(c).message();}
    public static Result executeResult(SkillContext c){
        var plan=plan(c);
        if(plan.isEmpty())return new Result(com.murilloskills.network.Protocol.ResultCode.NO_TARGET,"Nenhum bloco válido entre os pontos.",0);
        if(c.player().gameMode.isSurvival()&&c.player().getMainHandItem().getCount()<plan.size())return new Result(com.murilloskills.network.Protocol.ResultCode.MATERIALS,"Tenha "+plan.size()+" materiais na mão para confirmar.",0);
        String blocked=AreaTools.blocked(c,true,plan);
        if(!blocked.isEmpty())return new Result(AreaTools.failureCode(c,true,plan),blocked,0);
        int count=AreaTools.place(c,plan);if(count>0)clear(c);
        return new Result(count==plan.size()?com.murilloskills.network.Protocol.ResultCode.OK:count==0?com.murilloskills.network.Protocol.ResultCode.REJECTED:com.murilloskills.network.Protocol.ResultCode.PARTIAL,count>0?"Pincel: "+count+" blocos colocados.":"A colocação foi recusada; desfazer anterior preservado.",count);
    }
}
