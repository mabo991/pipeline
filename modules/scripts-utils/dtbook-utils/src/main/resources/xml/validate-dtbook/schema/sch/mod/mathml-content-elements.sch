<!--
        MathML in DAISY:
        "If any of the content elements listed in Chapter 4 of the MathML specification with the exception of those 
        listed in Section 4.4.11 (Semantic Mapping Elements) are used, they must appear inside of an 
        annotation-xml element inside of a semantics element."
        
        Here we test for the presence of the annotation-xml element. MathML already enforces how annotation-xml and semantics elements work together. 
-->
<pattern xmlns="http://purl.oclc.org/dsdl/schematron">
    <!-- this is a list of all the mathml 2 and 3 content elements except the semantic mapping elements -->
    <rule
        context="m:cn | 
        m:ci | 
        m:csymbol | 
        m:apply | 
        m:reln | 
        m:fn | 
        m:interval | 
        m:inverse | 
        m:sep | 
        m:condition | 
        m:declare | 
        m:lambda | 
        m:compose | 
        m:ident | 
        m:domain | 
        m:codomain | 
        m:image | 
        m:domainofapplication | 
        m:piecewise | 
        m:piece | 
        m:otherwise | 
        m:quotient | 
        m:factorial | 
        m:divide | 
        m:max |  
        m:min | 
        m:minus | 
        m:plus | 
        m:power | 
        m:rem | 
        m:times | 
        m:root | 
        m:gcd | 
        m:and | 
        m:or | 
        m:xor | 
        m:not | 
        m:implies | 
        m:forall | 
        m:exists | 
        m:abs | 
        m:conjugate | 
        m:arg | 
        m:real | 
        m:imaginary | 
        m:lcm | 
        m:floor | 
        m:ceiling | 
        m:eq | 
        m:neq | 
        m:gt | 
        m:lt | 
        m:geq | 
        m:leq | 
        m:equivalent | 
        m:approx | 
        m:factorof | 
        m:int | 
        m:diff | 
        m:partialdiff | 
        m:lowlimit | 
        m:uplimit | 
        m:bvar | 
        m:degree | 
        m:divergence | 
        m:grad | 
        m:curl | 
        m:laplacian | 
        m:set | 
        m:list | 
        m:union | 
        m:intersect | 
        m:in | 
        m:notin | 
        m:subset | 
        m:prsubset | 
        m:notsubset | 
        m:notprsubset | 
        m:setdiff | 
        m:card | 
        m:cartesianproduct | 
        m:sum | 
        m:product | 
        m:limit | 
        m:tendsto | 
        m:exp | 
        m:ln | 
        m:log | 
        m:sin | 
        m:cos | 
        m:tan | 
        m:sec | 
        m:csc | 
        m:cot | 
        m:sinh | 
        m:cosh | 
        m:tanh | 
        m:sech | 
        m:csch | 
        m:coth | 
        m:arcsin | 
        m:arccos | 
        m:arctan | 
        m:arccosh | 
        m:arccot | 
        m:arccoth | 
        m:arccsc | 
        m:arccsch | 
        m:arcsec | 
        m:arcsech | 
        m:arcsinh | 
        m:arctanh | 
        m:mean | 
        m:sdev | 
        m:variance | 
        m:median | 
        m:mode | 
        m:moment | 
        m:momentabout | 
        m:vector | 
        m:matrix | 
        m:matrixrow | 
        m:determinant | 
        m:transpose | 
        m:selector | 
        m:vectorproduct | 
        m:scalarproduct | 
        m:outerproduct | 
        m:integers | 
        m:reals | 
        m:rationals | 
        m:naturalnumbers | 
        m:complexes | 
        m:primes | 
        m:exponentiale | 
        m:imaginaryi | 
        m:notanumber | 
        m:true | 
        m:false | 
        m:emptyset | 
        m:pi | 
        m:eulergamma | 
        m:infinity | 
        m:cs |
        m:bind |
        m:share |
        m:cerror |
        m:cbytes ">
        <assert test="node()/ancestor::m:annotation-xml">Math content elements must have
            'annotation-xml' as ancestors.</assert>
    </rule>
</pattern>